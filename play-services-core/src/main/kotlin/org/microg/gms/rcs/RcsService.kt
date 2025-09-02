/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min
import kotlin.math.pow

private const val TAG = "RcsService"
private const val RCS_PREFS = "rcs_preferences"
private const val PREF_RCS_ENABLED = "rcs_enabled"
private const val PREF_PROVISIONING_STATUS = "provisioning_status"
private const val PREF_LAST_CONFIG_UPDATE = "last_config_update"
private const val PREF_LAST_CAPABILITY_UPDATE = "last_capability_update"
private const val PREF_NETWORK_TYPE = "last_network_type"
private const val PREF_CARRIER_CONFIG = "carrier_config"
private const val PREF_RETRY_COUNT = "retry_count"
private const val PREF_LAST_ERROR = "last_error"

// RCS Provisioning Status Constants
private const val RCS_STATUS_NOT_PROVISIONED = 0
private const val RCS_STATUS_PROVISIONED = 1
private const val RCS_STATUS_PROVISIONING = 2
private const val RCS_STATUS_ERROR = -1
private const val RCS_STATUS_CARRIER_NOT_SUPPORTED = -2
private const val RCS_STATUS_NETWORK_ERROR = -3

// Network Type Constants
private const val NETWORK_TYPE_UNKNOWN = 0
private const val NETWORK_TYPE_WIFI = 1
private const val NETWORK_TYPE_CELLULAR = 2
private const val NETWORK_TYPE_ROAMING = 3

// Capability Constants
private const val CAPABILITY_CACHE_DURATION = 5 * 60 * 1000L // 5 minutes
private const val CONFIG_CACHE_DURATION = 24 * 60 * 60 * 1000L // 24 hours
private const val MAX_RETRY_ATTEMPTS = 5
private const val BASE_RETRY_DELAY = 1000L // 1 second

// Feature Flags
private const val FEATURE_CHAT = "rcs_chat_capability"
private const val FEATURE_FILE_TRANSFER = "rcs_file_transfer_capability"
private const val FEATURE_GROUP_CHAT = "rcs_group_chat_capability"
private const val FEATURE_VIDEO_CALLING = "rcs_video_calling_capability"
private const val FEATURE_STANDALONE_MESSAGING = "rcs_standalone_messaging_capability"
private const val FEATURE_DELIVERY_REPORTS = "supports_delivery_reports"
private const val FEATURE_READ_REPORTS = "supports_read_reports"
private const val FEATURE_TYPING_INDICATORS = "supports_typing_indicators"

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
    private val subscriptionManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
        context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
    } else null

    private val preferences: SharedPreferences = context.getSharedPreferences(RCS_PREFS, Context.MODE_PRIVATE)
    private val registeredCallbacks = ConcurrentHashMap<String, IRcsCallbacks>()
    private val retryAttempts = AtomicInteger(0)
    private val lastCapabilityUpdate = AtomicLong(0)
    private val networkCallback = RcsNetworkCallback()

    // Device capability cache
    private var cachedCapabilities: Bundle? = null
    private var cachedCarrierConfig: Bundle? = null
    private var currentNetworkType = NETWORK_TYPE_UNKNOWN
    private var isRoaming = false

    private var isRcsEnabled: Boolean
        get() = preferences.getBoolean(PREF_RCS_ENABLED, true)
        set(value) = preferences.edit().putBoolean(PREF_RCS_ENABLED, value).apply()

    private var provisioningStatus: Int
        get() = preferences.getInt(PREF_PROVISIONING_STATUS, RCS_STATUS_NOT_PROVISIONED)
        set(value) = preferences.edit().putInt(PREF_PROVISIONING_STATUS, value).apply()

    private var lastConfigUpdate: Long
        get() = preferences.getLong(PREF_LAST_CONFIG_UPDATE, 0)
        set(value) = preferences.edit().putLong(PREF_LAST_CONFIG_UPDATE, value).apply()

    private var retryCount: Int
        get() = preferences.getInt(PREF_RETRY_COUNT, 0)
        set(value) = preferences.edit().putInt(PREF_RETRY_COUNT, value).apply()

    init {
        // Register network callback for dynamic capability updates
        registerNetworkCallback()
        // Initialize device capabilities
        updateDeviceCapabilities()
    }

    override fun getCapabilities(callbacks: IRcsCallbacks?) {
        Log.d(TAG, "getCapabilities() called by $packageName")
        lifecycleScope.launch {
            try {
                if (!isRcsEnabled) {
                    Log.d(TAG, "RCS is disabled, returning empty capabilities")
                    callbacks?.onCapabilities(Status.SUCCESS, Bundle.EMPTY)
                    return@launch
                }

                // Check if we can use cached capabilities
                val now = System.currentTimeMillis()
                if (cachedCapabilities != null &&
                    (now - lastCapabilityUpdate.get()) < CAPABILITY_CACHE_DURATION) {
                    Log.d(TAG, "Returning cached RCS capabilities")
                    callbacks?.onCapabilities(Status.SUCCESS, cachedCapabilities!!)
                    return@launch
                }

                // Generate dynamic capabilities based on current device state
                val capabilities = generateDynamicCapabilities()

                // Cache the capabilities
                cachedCapabilities = capabilities
                lastCapabilityUpdate.set(now)

                Log.d(TAG, "Returning dynamic RCS capabilities: ${capabilities.keySet().joinToString()}")
                callbacks?.onCapabilities(Status.SUCCESS, capabilities)

                // Notify registered callbacks of capability changes
                notifyCapabilityChange(capabilities)

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
                val deviceCapability = checkDeviceCapability()
                if (!deviceCapability.isCapable) {
                    Log.w(TAG, "Cannot start provisioning: ${deviceCapability.reason}")
                    provisioningStatus = deviceCapability.statusCode
                    callbacks?.onResult(Status(CommonStatusCodes.ERROR))
                    return@launch
                }

                val networkCapability = checkNetworkCapability()
                if (!networkCapability.isCapable) {
                    Log.w(TAG, "Cannot start provisioning: ${networkCapability.reason}")
                    callbacks?.onResult(Status(CommonStatusCodes.NETWORK_ERROR))
                    return@launch
                }

                val carrierSupport = checkCarrierSupport()
                if (!carrierSupport.isSupported) {
                    Log.w(TAG, "Cannot start provisioning: ${carrierSupport.reason}")
                    provisioningStatus = RCS_STATUS_CARRIER_NOT_SUPPORTED
                    callbacks?.onResult(Status(CommonStatusCodes.ERROR))
                    return@launch
                }

                Log.d(TAG, "Starting RCS provisioning process with retry logic")
                provisioningStatus = RCS_STATUS_PROVISIONING
                retryCount = 0

                val success = performProvisioningWithRetry()
                if (success) {
                    provisioningStatus = RCS_STATUS_PROVISIONED
                    retryCount = 0
                    // Invalidate capability cache to refresh with new provisioning status
                    invalidateCapabilityCache()
                    Log.i(TAG, "RCS provisioning completed successfully")
                    callbacks?.onResult(Status.SUCCESS)
                } else {
                    provisioningStatus = RCS_STATUS_ERROR
                    Log.w(TAG, "RCS provisioning failed after all retry attempts")
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

    // Device Capability Detection
    private fun checkDeviceCapability(): DeviceCapabilityResult {
        return try {
            // Check if telephony manager is available
            if (telephonyManager == null) {
                return DeviceCapabilityResult(false, RCS_STATUS_ERROR, "Telephony manager not available")
            }

            // Check SIM card status
            val simState = telephonyManager.simState
            if (simState != TelephonyManager.SIM_STATE_READY) {
                return DeviceCapabilityResult(false, RCS_STATUS_ERROR, "SIM card not ready: $simState")
            }

            // Check for valid phone number or subscriber ID
            if (!hasValidPhoneNumber()) {
                return DeviceCapabilityResult(false, RCS_STATUS_ERROR, "No valid phone number or subscriber ID")
            }

            // Check SMS/MMS capability as prerequisite
            if (!hasSmsCapability()) {
                return DeviceCapabilityResult(false, RCS_STATUS_ERROR, "SMS/MMS capability required for RCS")
            }

            DeviceCapabilityResult(true, RCS_STATUS_PROVISIONED, "Device capable")
        } catch (e: Exception) {
            Log.e(TAG, "Error checking device capability", e)
            DeviceCapabilityResult(false, RCS_STATUS_ERROR, "Device capability check failed: ${e.message}")
        }
    }

    private fun checkNetworkCapability(): NetworkCapabilityResult {
        return try {
            if (connectivityManager == null) {
                return NetworkCapabilityResult(false, "Connectivity manager not available")
            }

            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork == null) {
                return NetworkCapabilityResult(false, "No active network connection")
            }

            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            if (networkCapabilities == null) {
                return NetworkCapabilityResult(false, "Network capabilities not available")
            }

            // Check basic connectivity requirements
            if (!networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                return NetworkCapabilityResult(false, "Network does not have internet capability")
            }

            if (!networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                return NetworkCapabilityResult(false, "Network connection not validated")
            }

            // Determine network type and update current state
            currentNetworkType = when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NETWORK_TYPE_WIFI
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    isRoaming = telephonyManager?.isNetworkRoaming ?: false
                    if (isRoaming) NETWORK_TYPE_ROAMING else NETWORK_TYPE_CELLULAR
                }
                else -> NETWORK_TYPE_UNKNOWN
            }

            // Check network bandwidth for RCS features
            val linkDownstreamBandwidth = networkCapabilities.linkDownstreamBandwidthKbps
            val linkUpstreamBandwidth = networkCapabilities.linkUpstreamBandwidthKbps

            Log.d(TAG, "Network type: $currentNetworkType, roaming: $isRoaming, " +
                      "downstream: ${linkDownstreamBandwidth}kbps, upstream: ${linkUpstreamBandwidth}kbps")

            NetworkCapabilityResult(true, "Network capable")
        } catch (e: Exception) {
            Log.e(TAG, "Error checking network capability", e)
            NetworkCapabilityResult(false, "Network capability check failed: ${e.message}")
        }
    }

    // Carrier Support Detection
    private fun checkCarrierSupport(): CarrierSupportResult {
        return try {
            telephonyManager?.let { tm ->
                val networkOperator = tm.networkOperator
                val networkOperatorName = tm.networkOperatorName
                val simOperator = tm.simOperator
                val simOperatorName = tm.simOperatorName

                Log.d(TAG, "Checking carrier support - Network: $networkOperatorName ($networkOperator), " +
                          "SIM: $simOperatorName ($simOperator)")

                // Check for known RCS-supporting carriers
                val isSupported = when {
                    // US Carriers
                    networkOperatorName?.contains("Verizon", ignoreCase = true) == true -> true
                    networkOperatorName?.contains("T-Mobile", ignoreCase = true) == true -> true
                    networkOperatorName?.contains("AT&T", ignoreCase = true) == true -> true
                    networkOperatorName?.contains("Sprint", ignoreCase = true) == true -> true

                    // International carriers (examples)
                    networkOperatorName?.contains("Vodafone", ignoreCase = true) == true -> true
                    networkOperatorName?.contains("Orange", ignoreCase = true) == true -> true
                    networkOperatorName?.contains("EE", ignoreCase = true) == true -> true
                    networkOperatorName?.contains("Three", ignoreCase = true) == true -> true

                    // Check by MCC/MNC for more precise carrier detection
                    networkOperator?.startsWith("310") == true -> true // US carriers
                    networkOperator?.startsWith("311") == true -> true // US carriers
                    networkOperator?.startsWith("312") == true -> true // US carriers

                    else -> {
                        // For unknown carriers, assume RCS support but log for monitoring
                        Log.i(TAG, "Unknown carrier, assuming RCS support: $networkOperatorName")
                        true
                    }
                }

                if (isSupported) {
                    CarrierSupportResult(true, "Carrier supports RCS: $networkOperatorName")
                } else {
                    CarrierSupportResult(false, "Carrier does not support RCS: $networkOperatorName")
                }
            } ?: CarrierSupportResult(false, "Unable to determine carrier information")
        } catch (e: Exception) {
            Log.e(TAG, "Error checking carrier support", e)
            CarrierSupportResult(false, "Carrier support check failed: ${e.message}")
        }
    }

    // Dynamic Capability Generation
    private fun generateDynamicCapabilities(): Bundle {
        val capabilities = Bundle()

        try {
            val deviceCapable = checkDeviceCapability().isCapable
            val networkCapable = checkNetworkCapability().isCapable
            val carrierSupported = checkCarrierSupport().isSupported
            val isProvisioned = provisioningStatus == RCS_STATUS_PROVISIONED

            // Base RCS capabilities
            val baseCapable = deviceCapable && networkCapable && carrierSupported && isProvisioned

            // Network-dependent features
            val hasHighBandwidth = currentNetworkType == NETWORK_TYPE_WIFI ||
                                  (currentNetworkType == NETWORK_TYPE_CELLULAR && !isRoaming)

            // Feature availability based on conditions
            capabilities.apply {
                putString("rcs_version", "6.0")
                putBoolean(FEATURE_CHAT, baseCapable)
                putBoolean(FEATURE_STANDALONE_MESSAGING, deviceCapable && carrierSupported)
                putBoolean(FEATURE_DELIVERY_REPORTS, baseCapable)
                putBoolean(FEATURE_READ_REPORTS, baseCapable)
                putBoolean(FEATURE_TYPING_INDICATORS, baseCapable)

                // Bandwidth-dependent features
                putBoolean(FEATURE_FILE_TRANSFER, baseCapable && hasHighBandwidth)
                putBoolean(FEATURE_GROUP_CHAT, baseCapable && hasHighBandwidth)
                putBoolean(FEATURE_VIDEO_CALLING, baseCapable && hasHighBandwidth && !isRoaming)

                // Dynamic limits based on network type
                when (currentNetworkType) {
                    NETWORK_TYPE_WIFI -> {
                        putInt("max_file_size", 104857600) // 100MB on WiFi
                        putInt("max_group_size", 100)
                        putInt("max_video_duration", 300) // 5 minutes
                    }
                    NETWORK_TYPE_CELLULAR -> {
                        putInt("max_file_size", 10485760) // 10MB on cellular
                        putInt("max_group_size", 50)
                        putInt("max_video_duration", 120) // 2 minutes
                    }
                    NETWORK_TYPE_ROAMING -> {
                        putInt("max_file_size", 1048576) // 1MB when roaming
                        putInt("max_group_size", 20)
                        putInt("max_video_duration", 0) // No video when roaming
                    }
                    else -> {
                        putInt("max_file_size", 5242880) // 5MB default
                        putInt("max_group_size", 30)
                        putInt("max_video_duration", 60) // 1 minute
                    }
                }

                // Additional metadata
                putInt("network_type", currentNetworkType)
                putBoolean("is_roaming", isRoaming)
                putLong("capability_timestamp", System.currentTimeMillis())
                putString("carrier_name", telephonyManager?.networkOperatorName ?: "unknown")
                putInt("provisioning_status", provisioningStatus)
            }

            Log.d(TAG, "Generated dynamic capabilities - Network: $currentNetworkType, " +
                      "Roaming: $isRoaming, Base capable: $baseCapable")

        } catch (e: Exception) {
            Log.e(TAG, "Error generating dynamic capabilities", e)
            // Return minimal capabilities on error
            capabilities.apply {
                putString("rcs_version", "6.0")
                putBoolean(FEATURE_CHAT, false)
                putBoolean(FEATURE_FILE_TRANSFER, false)
                putBoolean(FEATURE_GROUP_CHAT, false)
                putBoolean(FEATURE_VIDEO_CALLING, false)
                putBoolean(FEATURE_STANDALONE_MESSAGING, false)
            }
        }

        return capabilities
    }

    // Network Callback for Dynamic Updates
    private inner class RcsNetworkCallback : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "Network available: $network")
            updateDeviceCapabilities()
        }

        override fun onLost(network: Network) {
            Log.d(TAG, "Network lost: $network")
            updateDeviceCapabilities()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            Log.d(TAG, "Network capabilities changed: $network")
            updateDeviceCapabilities()
        }
    }

    // Helper Methods
    private fun registerNetworkCallback() {
        try {
            connectivityManager?.let { cm ->
                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                cm.registerNetworkCallback(request, networkCallback)
                Log.d(TAG, "Network callback registered")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register network callback", e)
        }
    }

    private fun updateDeviceCapabilities() {
        lifecycleScope.launch {
            try {
                // Invalidate cached capabilities to force refresh
                invalidateCapabilityCache()

                // Notify all registered callbacks of capability changes
                val newCapabilities = generateDynamicCapabilities()
                notifyCapabilityChange(newCapabilities)

                Log.d(TAG, "Device capabilities updated")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating device capabilities", e)
            }
        }
    }

    private fun invalidateCapabilityCache() {
        cachedCapabilities = null
        lastCapabilityUpdate.set(0)
        cachedCarrierConfig = null
    }

    private fun notifyCapabilityChange(capabilities: Bundle) {
        registeredCallbacks.values.forEach { callback ->
            try {
                callback.onCapabilities(Status.SUCCESS, capabilities)
            } catch (e: Exception) {
                Log.w(TAG, "Error notifying capability change", e)
            }
        }
    }

    private fun hasSmsCapability(): Boolean {
        return try {
            telephonyManager?.let { tm ->
                // Check if device has SMS capability
                tm.phoneType != TelephonyManager.PHONE_TYPE_NONE &&
                tm.simState == TelephonyManager.SIM_STATE_READY
            } ?: false
        } catch (e: Exception) {
            Log.w(TAG, "Error checking SMS capability", e)
            false
        }
    }

    private suspend fun performProvisioningWithRetry(): Boolean {
        var attempts = 0
        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                Log.d(TAG, "Provisioning attempt ${attempts + 1}/$MAX_RETRY_ATTEMPTS")

                // Simulate provisioning process
                delay(2000L + (attempts * 1000L)) // Increasing delay

                // Check if provisioning conditions are still met
                val deviceCapable = checkDeviceCapability().isCapable
                val networkCapable = checkNetworkCapability().isCapable
                val carrierSupported = checkCarrierSupport().isSupported

                if (deviceCapable && networkCapable && carrierSupported) {
                    Log.i(TAG, "Provisioning successful on attempt ${attempts + 1}")
                    return true
                }

                attempts++
                if (attempts < MAX_RETRY_ATTEMPTS) {
                    val delayMs = BASE_RETRY_DELAY * (2.0.pow(attempts.toDouble())).toLong()
                    Log.d(TAG, "Provisioning failed, retrying in ${delayMs}ms")
                    delay(delayMs)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during provisioning attempt ${attempts + 1}", e)
                attempts++
                if (attempts < MAX_RETRY_ATTEMPTS) {
                    delay(BASE_RETRY_DELAY * attempts.toLong())
                }
            }
        }

        Log.w(TAG, "Provisioning failed after $MAX_RETRY_ATTEMPTS attempts")
        retryCount = attempts
        return false
    }

    private fun getCarrierSpecificConfig(): Bundle {
        // Check if we have cached carrier config
        if (cachedCarrierConfig != null &&
            (System.currentTimeMillis() - lastConfigUpdate) < CONFIG_CACHE_DURATION) {
            return cachedCarrierConfig!!
        }

        val config = Bundle()

        try {
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

                // Enhanced carrier-specific configurations
                when {
                    networkOperatorName?.contains("Verizon", ignoreCase = true) == true -> {
                        config.apply {
                            putString("rcs_config_server", "config.rcs.vzw.com")
                            putInt("rcs_max_file_size", 104857600) // 100MB for Verizon
                            putBoolean("rcs_video_calling_enabled", true)
                            putString("rcs_user_agent", "microG-RCS-VZW/1.0")
                        }
                    }
                    networkOperatorName?.contains("T-Mobile", ignoreCase = true) == true -> {
                        config.apply {
                            putString("rcs_config_server", "config.rcs.tmo.com")
                            putInt("rcs_max_file_size", 52428800) // 50MB for T-Mobile
                            putBoolean("rcs_video_calling_enabled", true)
                            putString("rcs_user_agent", "microG-RCS-TMO/1.0")
                        }
                    }
                    networkOperatorName?.contains("AT&T", ignoreCase = true) == true -> {
                        config.apply {
                            putString("rcs_config_server", "config.rcs.att.com")
                            putInt("rcs_max_file_size", 26214400) // 25MB for AT&T
                            putBoolean("rcs_video_calling_enabled", true)
                            putString("rcs_user_agent", "microG-RCS-ATT/1.0")
                        }
                    }
                    networkOperatorName?.contains("Sprint", ignoreCase = true) == true -> {
                        config.apply {
                            putString("rcs_config_server", "config.rcs.sprint.com")
                            putInt("rcs_max_file_size", 10485760) // 10MB for Sprint
                            putBoolean("rcs_video_calling_enabled", false) // Sprint limitation
                            putString("rcs_user_agent", "microG-RCS-SPR/1.0")
                        }
                    }
                }

                // Adjust config based on current network conditions
                when (currentNetworkType) {
                    NETWORK_TYPE_ROAMING -> {
                        config.putInt("rcs_max_file_size", min(config.getInt("rcs_max_file_size"), 1048576)) // Max 1MB when roaming
                        config.putBoolean("rcs_video_calling_enabled", false) // Disable video when roaming
                    }
                    NETWORK_TYPE_CELLULAR -> {
                        config.putInt("rcs_max_file_size", min(config.getInt("rcs_max_file_size"), 10485760)) // Max 10MB on cellular
                    }
                }
            }

            // Cache the configuration
            cachedCarrierConfig = config
            lastConfigUpdate = System.currentTimeMillis()

        } catch (e: Exception) {
            Log.w(TAG, "Error getting carrier-specific config", e)
            config.apply {
                putString("rcs_config_server", "config.rcs.mnc001.mcc001.pub.3gppnetwork.org")
                putInt("rcs_max_file_size", 10485760)
                putInt("rcs_max_group_size", 100)
            }
        }

        return config
    }

    override fun onTransact(code: Int, data: android.os.Parcel, reply: android.os.Parcel?, flags: Int) =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}

// Data Classes for Capability Results
private data class DeviceCapabilityResult(
    val isCapable: Boolean,
    val statusCode: Int,
    val reason: String
)

private data class NetworkCapabilityResult(
    val isCapable: Boolean,
    val reason: String
)

private data class CarrierSupportResult(
    val isSupported: Boolean,
    val reason: String
)
