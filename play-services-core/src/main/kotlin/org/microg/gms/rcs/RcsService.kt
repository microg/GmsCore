/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs

import android.content.Context
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

private const val TAG = "RcsService"

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

    override fun getCapabilities(callbacks: IRcsCallbacks?) {
        Log.d(TAG, "getCapabilities() called by $packageName")
        lifecycleScope.launch {
            try {
                val capabilities = Bundle().apply {
                    putBoolean("rcs_chat_capability", true)
                    putBoolean("rcs_file_transfer_capability", true)
                    putBoolean("rcs_group_chat_capability", true)
                    putBoolean("rcs_standalone_messaging_capability", true)
                    putString("rcs_version", "6.0")
                }
                callbacks?.onCapabilities(Status.SUCCESS, capabilities)
            } catch (e: Exception) {
                Log.w(TAG, "Error in getCapabilities", e)
                callbacks?.onCapabilities(Status(CommonStatusCodes.INTERNAL_ERROR), Bundle.EMPTY)
            }
        }
    }

    override fun isAvailable(callbacks: IRcsCallbacks?) {
        Log.d(TAG, "isAvailable() called by $packageName")
        lifecycleScope.launch {
            try {
                // Check if we have a valid phone number and network connectivity
                val hasPhoneNumber = telephonyManager?.line1Number?.isNotEmpty() == true ||
                                   telephonyManager?.subscriberId?.isNotEmpty() == true
                val isAvailable = hasPhoneNumber && isNetworkAvailable()
                
                Log.d(TAG, "RCS availability: $isAvailable (hasPhoneNumber: $hasPhoneNumber)")
                callbacks?.onAvailability(Status.SUCCESS, isAvailable)
            } catch (e: Exception) {
                Log.w(TAG, "Error checking RCS availability", e)
                callbacks?.onAvailability(Status.SUCCESS, true) // Default to available
            }
        }
    }

    override fun getConfiguration(callbacks: IRcsCallbacks?) {
        Log.d(TAG, "getConfiguration() called by $packageName")
        lifecycleScope.launch {
            try {
                val config = Bundle().apply {
                    putString("rcs_config_server", "config.rcs.mnc001.mcc001.pub.3gppnetwork.org")
                    putInt("rcs_config_version", 1)
                    putBoolean("rcs_config_valid", true)
                    putString("rcs_user_agent", "microG-RCS/1.0")
                    putInt("rcs_max_file_size", 10485760) // 10MB
                    putInt("rcs_max_group_size", 100)
                }
                callbacks?.onConfiguration(Status.SUCCESS, config)
            } catch (e: Exception) {
                Log.w(TAG, "Error in getConfiguration", e)
                callbacks?.onConfiguration(Status(CommonStatusCodes.INTERNAL_ERROR), Bundle.EMPTY)
            }
        }
    }

    override fun registerCapabilityCallback(callbacks: IRcsCallbacks?) {
        Log.d(TAG, "registerCapabilityCallback() called by $packageName")
        lifecycleScope.launch {
            try {
                callbacks?.onResult(Status.SUCCESS)
            } catch (e: Exception) {
                Log.w(TAG, "Error in registerCapabilityCallback", e)
                callbacks?.onResult(Status(CommonStatusCodes.INTERNAL_ERROR))
            }
        }
    }

    override fun unregisterCapabilityCallback(callbacks: IRcsCallbacks?) {
        Log.d(TAG, "unregisterCapabilityCallback() called by $packageName")
        lifecycleScope.launch {
            try {
                callbacks?.onResult(Status.SUCCESS)
            } catch (e: Exception) {
                Log.w(TAG, "Error in unregisterCapabilityCallback", e)
                callbacks?.onResult(Status(CommonStatusCodes.INTERNAL_ERROR))
            }
        }
    }

    override fun startProvisioning(callbacks: IRcsCallbacks?) {
        Log.d(TAG, "startProvisioning() called by $packageName")
        lifecycleScope.launch {
            try {
                // Simulate successful provisioning for microG compatibility
                callbacks?.onResult(Status.SUCCESS)
            } catch (e: Exception) {
                Log.w(TAG, "Error in startProvisioning", e)
                callbacks?.onResult(Status(CommonStatusCodes.INTERNAL_ERROR))
            }
        }
    }

    override fun stopProvisioning(callbacks: IRcsCallbacks?) {
        Log.d(TAG, "stopProvisioning() called by $packageName")
        lifecycleScope.launch {
            try {
                callbacks?.onResult(Status.SUCCESS)
            } catch (e: Exception) {
                Log.w(TAG, "Error in stopProvisioning", e)
                callbacks?.onResult(Status(CommonStatusCodes.INTERNAL_ERROR))
            }
        }
    }

    override fun getProvisioningStatus(callbacks: IRcsCallbacks?) {
        Log.d(TAG, "getProvisioningStatus() called by $packageName")
        lifecycleScope.launch {
            try {
                // Return provisioned status (1 = provisioned, 0 = not provisioned)
                val status = if (isNetworkAvailable()) 1 else 0
                callbacks?.onProvisioningStatus(Status.SUCCESS, status)
            } catch (e: Exception) {
                Log.w(TAG, "Error in getProvisioningStatus", e)
                callbacks?.onProvisioningStatus(Status(CommonStatusCodes.INTERNAL_ERROR), 0)
            }
        }
    }

    override fun triggerReconfiguration(callbacks: IRcsCallbacks?) {
        Log.d(TAG, "triggerReconfiguration() called by $packageName")
        lifecycleScope.launch {
            try {
                callbacks?.onResult(Status.SUCCESS)
            } catch (e: Exception) {
                Log.w(TAG, "Error in triggerReconfiguration", e)
                callbacks?.onResult(Status(CommonStatusCodes.INTERNAL_ERROR))
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) 
                as? android.net.ConnectivityManager
            val activeNetwork = connectivityManager?.activeNetworkInfo
            activeNetwork?.isConnected == true
        } catch (e: Exception) {
            Log.w(TAG, "Error checking network availability", e)
            true // Default to available
        }
    }

    override fun onTransact(code: Int, data: android.os.Parcel, reply: android.os.Parcel?, flags: Int) =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
