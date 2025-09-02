/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.rcs.internal.IRcsCallbacks
import com.google.android.gms.rcs.internal.IRcsService
import kotlinx.coroutines.launch
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "RcsService"
private const val RCS_PREFS = "rcs_preferences"
private const val PREF_RCS_ENABLED = "rcs_enabled"
private const val PREF_PROVISIONING_STATUS = "provisioning_status"
private const val PREF_LAST_CONFIG_UPDATE = "last_config_update"

// RCS Provisioning Status Constants
private const val RCS_STATUS_NOT_PROVISIONED = 0
private const val RCS_STATUS_PROVISIONED = 1
private const val RCS_STATUS_PROVISIONING = 2
private const val RCS_STATUS_ERROR = -1

class RcsService : BaseService(TAG, GmsService.RCS) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
        Log.d(TAG, "handleServiceRequest for package: $packageName")
        
        callback.onPostInitCompleteWithConnectionInfo(
            CommonStatusCodes.SUCCESS,
            RcsServiceImpl(this, packageName ?: "unknown", lifecycle),
            ConnectionInfo().apply {
                features = arrayOf(
                    Feature("rcs_capabilities", 1),
                    Feature("rcs_provisioning", 1),
                    Feature("rcs_configuration", 1)
                )
            }
        )
    }
}

class RcsServiceImpl(
    private val context: Context,
    private val packageName: String,
    override val lifecycle: Lifecycle
) : IRcsService.Stub(), LifecycleOwner {

    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val preferences: SharedPreferences = context.getSharedPreferences(RCS_PREFS, Context.MODE_PRIVATE)
    private val registeredCallbacks = ConcurrentHashMap<String, IRcsCallbacks>()

    private var isRcsEnabled: Boolean
        get() = preferences.getBoolean(PREF_RCS_ENABLED, true)
        set(value) = preferences.edit().putBoolean(PREF_RCS_ENABLED, value).apply()

    private var provisioningStatus: Int
        get() = preferences.getInt(PREF_PROVISIONING_STATUS, RCS_STATUS_NOT_PROVISIONED)
        set(value) = preferences.edit().putInt(PREF_PROVISIONING_STATUS, value).apply()

    private var lastConfigUpdate: Long
        get() = preferences.getLong(PREF_LAST_CONFIG_UPDATE, 0)
        set(value) = preferences.edit().putLong(PREF_LAST_CONFIG_UPDATE, value).apply()

    override fun getCapabilities(callbacks: IRcsCallbacks?) {
        Log.d(TAG, "getCapabilities() called by $packageName")
        lifecycleScope.launch {
            try {
                if (!isRcsEnabled) {
                    Log.d(TAG, "RCS is disabled, returning empty capabilities")
                    callbacks?.onCapabilities(Status.SUCCESS, Bundle.EMPTY)
                    return@launch
                }

                val capabilities = Bundle().apply {
                    // Core RCS capabilities based on device and network support
                    putBoolean("rcs_chat_capability", hasValidPhoneNumber() && isNetworkAvailable())
                    putBoolean("rcs_file_transfer_capability", hasValidPhoneNumber() && isNetworkAvailable())
                    putBoolean("rcs_group_chat_capability", hasValidPhoneNumber() && isNetworkAvailable())
                    putBoolean("rcs_standalone_messaging_capability", hasValidPhoneNumber())
                    putString("rcs_version", "6.0")
                    putInt("max_file_size", 10485760) // 10MB
                    putInt("max_group_size", 100)
                    putBoolean("supports_delivery_reports", true)
                    putBoolean("supports_read_reports", true)
                    putBoolean("supports_typing_indicators", true)
                }

                Log.d(TAG, "Returning RCS capabilities: ${capabilities.keySet().joinToString()}")
                callbacks?.onCapabilities(Status.SUCCESS, capabilities)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting RCS capabilities", e)
                callbacks?.onCapabilities(Status(CommonStatusCodes.INTERNAL_ERROR), Bundle.EMPTY)
            }
        }
    }

    override fun isAvailable(callbacks: IRcsCallbacks?) {
        Log.d(TAG, "isAvailable() called by $packageName")
        lifecycleScope.launch {
            try {
                if (!isRcsEnabled) {
                    Log.d(TAG, "RCS is disabled by user preference")
                    callbacks?.onAvailability(Status.SUCCESS, false)
                    return@launch
                }

                val hasPhoneNumber = hasValidPhoneNumber()
                val hasNetwork = isNetworkAvailable()
                val isProvisioned = provisioningStatus == RCS_STATUS_PROVISIONED
                val isAvailable = hasPhoneNumber && hasNetwork && isProvisioned

                Log.d(TAG, "RCS availability check: hasPhone=$hasPhoneNumber, hasNetwork=$hasNetwork, isProvisioned=$isProvisioned, available=$isAvailable")
                callbacks?.onAvailability(Status.SUCCESS, isAvailable)
            } catch (e: Exception) {
                Log.e(TAG, "Error checking RCS availability", e)
                callbacks?.onAvailability(Status(CommonStatusCodes.INTERNAL_ERROR), false)
            }
        }
    }

    override fun getConfiguration(callbacks: IRcsCallbacks?) {
        Log.d(TAG, "getConfiguration() called by $packageName")
        lifecycleScope.launch {
            try {
                if (!isRcsEnabled) {
                    Log.d(TAG, "RCS is disabled, returning empty configuration")
                    callbacks?.onConfiguration(Status(CommonStatusCodes.ERROR), Bundle.EMPTY)
                    return@launch
                }

                // Get carrier-specific configuration based on SIM
                val carrierConfig = getCarrierSpecificConfig()
                val config = Bundle().apply {
                    putAll(carrierConfig)
                    putString("rcs_user_agent", "microG-RCS/1.0")
                    putLong("config_timestamp", System.currentTimeMillis())
                    putBoolean("rcs_config_valid", true)
                    putInt("rcs_config_version", 2)
                }

                // Update last config timestamp
                lastConfigUpdate = System.currentTimeMillis()

                Log.d(TAG, "Returning RCS configuration with ${config.size()} parameters")
                callbacks?.onConfiguration(Status.SUCCESS, config)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting RCS configuration", e)
                callbacks?.onConfiguration(Status(CommonStatusCodes.INTERNAL_ERROR), Bundle.EMPTY)
            }
        }
    }

    override fun registerCapabilityCallback(callbacks: IRcsCallbacks?) {
        Log.d(TAG, "registerCapabilityCallback() called by $packageName")
        lifecycleScope.launch {
            try {
                if (callbacks != null) {
                    val callbackKey = "${packageName}_${System.currentTimeMillis()}"
                    registeredCallbacks[callbackKey] = callbacks
                    Log.d(TAG, "Registered capability callback for $packageName (key: $callbackKey)")
                    callbacks.onResult(Status.SUCCESS)
                } else {
                    Log.w(TAG, "Attempted to register null callback")
                    callbacks?.onResult(Status(CommonStatusCodes.ERROR))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error registering capability callback", e)
                callbacks?.onResult(Status(CommonStatusCodes.INTERNAL_ERROR))
            }
        }
    }

    override fun unregisterCapabilityCallback(callbacks: IRcsCallbacks?) {
        Log.d(TAG, "unregisterCapabilityCallback() called by $packageName")
        lifecycleScope.launch {
            try {
                if (callbacks != null) {
                    // Find and remove the callback
                    val keysToRemove = registeredCallbacks.entries
                        .filter { it.value == callbacks }
                        .map { it.key }

                    keysToRemove.forEach { key ->
                        registeredCallbacks.remove(key)
                        Log.d(TAG, "Unregistered capability callback for $packageName (key: $key)")
                    }

                    callbacks.onResult(Status.SUCCESS)
                } else {
                    callbacks?.onResult(Status(CommonStatusCodes.ERROR))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering capability callback", e)
                callbacks?.onResult(Status(CommonStatusCodes.INTERNAL_ERROR))
            }
        }
    }

    override fun startProvisioning(callbacks: IRcsCallbacks?) {
        Log.d(TAG, "startProvisioning() called by $packageName")
        lifecycleScope.launch {
            try {
                if (!hasValidPhoneNumber()) {
                    Log.w(TAG, "Cannot start provisioning: no valid phone number")
                    callbacks?.onResult(Status(CommonStatusCodes.ERROR))
                    return@launch
                }

                if (!isNetworkAvailable()) {
                    Log.w(TAG, "Cannot start provisioning: no network connectivity")
                    callbacks?.onResult(Status(CommonStatusCodes.NETWORK_ERROR))
                    return@launch
                }

                Log.d(TAG, "Starting RCS provisioning process")
                provisioningStatus = RCS_STATUS_PROVISIONING

                // Simulate provisioning process with realistic delay
                kotlinx.coroutines.delay(2000)

                // For microG compatibility, we'll mark as provisioned if basic requirements are met
                if (hasValidPhoneNumber() && isNetworkAvailable()) {
                    provisioningStatus = RCS_STATUS_PROVISIONED
                    Log.i(TAG, "RCS provisioning completed successfully")
                    callbacks?.onResult(Status.SUCCESS)
                } else {
                    provisioningStatus = RCS_STATUS_ERROR
                    Log.w(TAG, "RCS provisioning failed: requirements not met")
                    callbacks?.onResult(Status(CommonStatusCodes.ERROR))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during RCS provisioning", e)
                provisioningStatus = RCS_STATUS_ERROR
                callbacks?.onResult(Status(CommonStatusCodes.INTERNAL_ERROR))
            }
        }
    }

    override fun stopProvisioning(callbacks: IRcsCallbacks?) {
        Log.d(TAG, "stopProvisioning() called by $packageName")
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Stopping RCS provisioning process")
                provisioningStatus = RCS_STATUS_NOT_PROVISIONED
                callbacks?.onResult(Status.SUCCESS)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping RCS provisioning", e)
                callbacks?.onResult(Status(CommonStatusCodes.INTERNAL_ERROR))
            }
        }
    }

    override fun getProvisioningStatus(callbacks: IRcsCallbacks?) {
        Log.d(TAG, "getProvisioningStatus() called by $packageName")
        lifecycleScope.launch {
            try {
                val currentStatus = provisioningStatus
                Log.d(TAG, "Current RCS provisioning status: $currentStatus")
                callbacks?.onProvisioningStatus(Status.SUCCESS, currentStatus)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting RCS provisioning status", e)
                callbacks?.onProvisioningStatus(Status(CommonStatusCodes.INTERNAL_ERROR), RCS_STATUS_ERROR)
            }
        }
    }

    override fun triggerReconfiguration(callbacks: IRcsCallbacks?) {
        Log.d(TAG, "triggerReconfiguration() called by $packageName")
        lifecycleScope.launch {
            try {
                if (!isNetworkAvailable()) {
                    Log.w(TAG, "Cannot trigger reconfiguration: no network connectivity")
                    callbacks?.onResult(Status(CommonStatusCodes.NETWORK_ERROR))
                    return@launch
                }

                Log.d(TAG, "Triggering RCS reconfiguration")

                // Reset configuration timestamp to force refresh
                lastConfigUpdate = 0

                // If currently provisioned, trigger a re-provisioning
                if (provisioningStatus == RCS_STATUS_PROVISIONED) {
                    provisioningStatus = RCS_STATUS_PROVISIONING

                    // Simulate reconfiguration delay
                    kotlinx.coroutines.delay(1500)

                    if (hasValidPhoneNumber() && isNetworkAvailable()) {
                        provisioningStatus = RCS_STATUS_PROVISIONED
                        lastConfigUpdate = System.currentTimeMillis()
                        Log.i(TAG, "RCS reconfiguration completed successfully")
                    } else {
                        provisioningStatus = RCS_STATUS_ERROR
                        Log.w(TAG, "RCS reconfiguration failed")
                    }
                }

                callbacks?.onResult(Status.SUCCESS)
            } catch (e: Exception) {
                Log.e(TAG, "Error during RCS reconfiguration", e)
                callbacks?.onResult(Status(CommonStatusCodes.INTERNAL_ERROR))
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            connectivityManager?.let { cm ->
                val activeNetwork = cm.activeNetwork ?: return false
                val networkCapabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false

                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
                (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                 networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
            } ?: false
        } catch (e: Exception) {
            Log.w(TAG, "Error checking network availability", e)
            false
        }
    }

    private fun hasValidPhoneNumber(): Boolean {
        return try {
            telephonyManager?.let { tm ->
                val line1Number = tm.line1Number
                val subscriberId = tm.subscriberId
                val simState = tm.simState

                // Check if SIM is ready and we have some form of phone identification
                simState == TelephonyManager.SIM_STATE_READY &&
                (!line1Number.isNullOrEmpty() || !subscriberId.isNullOrEmpty())
            } ?: false
        } catch (e: Exception) {
            Log.w(TAG, "Error checking phone number validity", e)
            false
        }
    }

    private fun getCarrierSpecificConfig(): Bundle {
        return try {
            val config = Bundle()

            telephonyManager?.let { tm ->
                val networkOperator = tm.networkOperator
                val networkOperatorName = tm.networkOperatorName

                // Default RCS configuration
                config.apply {
                    putString("rcs_config_server", "config.rcs.mnc001.mcc001.pub.3gppnetwork.org")
                    putInt("rcs_max_file_size", 10485760) // 10MB
                    putInt("rcs_max_group_size", 100)
                    putInt("rcs_session_timer", 90)
                    putBoolean("rcs_file_transfer_enabled", true)
                    putBoolean("rcs_group_chat_enabled", true)
                    putBoolean("rcs_delivery_reports_enabled", true)
                    putBoolean("rcs_read_reports_enabled", true)
                    putBoolean("rcs_typing_indicators_enabled", true)
                    putString("rcs_network_operator", networkOperator ?: "unknown")
                    putString("rcs_network_operator_name", networkOperatorName ?: "unknown")
                }

                // Carrier-specific configurations could be added here
                when {
                    networkOperatorName?.contains("Verizon", ignoreCase = true) == true -> {
                        config.putString("rcs_config_server", "config.rcs.vzw.com")
                    }
                    networkOperatorName?.contains("T-Mobile", ignoreCase = true) == true -> {
                        config.putString("rcs_config_server", "config.rcs.tmo.com")
                    }
                    networkOperatorName?.contains("AT&T", ignoreCase = true) == true -> {
                        config.putString("rcs_config_server", "config.rcs.att.com")
                    }
                }
            }

            config
        } catch (e: Exception) {
            Log.w(TAG, "Error getting carrier-specific config", e)
            Bundle().apply {
                putString("rcs_config_server", "config.rcs.mnc001.mcc001.pub.3gppnetwork.org")
                putInt("rcs_max_file_size", 10485760)
                putInt("rcs_max_group_size", 100)
            }
        }
    }

    override fun onTransact(code: Int, data: android.os.Parcel, reply: android.os.Parcel?, flags: Int) =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
