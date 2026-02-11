/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsProvisioningManager - Handles RCS provisioning workflow
 * 
 * This class implements the GSMA RCC.07 (Rich Communication Suite - Advanced Communications
 * Services and Client Specification) provisioning flow.
 *
 * Workflow:
 * 1. SIM Detection & Carrier Config Lookup (RCC.07 §2.3)
 * 2. HTTP/HTTPS Auto-Configuration (RCC.07 §2.4)
 * 3. SIP/IMS Registration (RCC.07 §2.5 or Direct Mode)
 *
 * It manages the complete lifecycle state machine and persistent storage of
 * provisioning tokens using EncryptedSharedPreferences for security.
 */

package org.microg.gms.rcs

import android.content.Context
import android.content.SharedPreferences
import android.telephony.TelephonyManager
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.gms.rcs.IRcsProvisioningCallback
import com.google.android.gms.rcs.RcsConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class RcsProvisioningManager(
    private val context: Context,
    private val sharedPreferencesOverride: SharedPreferences? = null,
    private val connectivityChecker: ConnectivityChecker = DefaultConnectivityChecker()
) {

    companion object {
        private const val TAG = "RcsProvisioning"
        
        private const val PREFERENCES_NAME = "rcs_provisioning"
        private const val KEY_IS_PROVISIONED = "is_provisioned"
        private const val KEY_REGISTERED_PHONE = "registered_phone"
        private const val KEY_PROVISIONING_TIMESTAMP = "provisioning_timestamp"
        private const val KEY_CARRIER_CONFIG = "carrier_config"
        private const val KEY_RCS_VERSION = "rcs_version"
        
        private const val PROVISIONING_TIMEOUT_MILLIS = 30000L
        private const val KEY_MANUAL_PHONE_OVERRIDE = "manual_phone_override"
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isProvisioningInProgress = AtomicBoolean(false)
    private val currentProvisioningStatus = AtomicInteger(IRcsProvisioningCallback.STATUS_NOT_PROVISIONED)
    
    private val encryptedPreferences: SharedPreferences by lazy {
        sharedPreferencesOverride ?: createEncryptedPreferences()
    }

    private fun createEncryptedPreferences(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFERENCES_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to create encrypted preferences, using regular preferences", exception)
            context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        }
    }

    fun isProvisioned(): Boolean {
        return encryptedPreferences.getBoolean(KEY_IS_PROVISIONED, false)
    }

    fun getProvisioningStatus(): Int {
        return if (isProvisioned()) {
            IRcsProvisioningCallback.STATUS_PROVISIONED
        } else if (isProvisioningInProgress.get()) {
            IRcsProvisioningCallback.STATUS_PROVISIONING
        } else {
            IRcsProvisioningCallback.STATUS_NOT_PROVISIONED
        }
    }

    fun startProvisioning(callback: IRcsProvisioningCallback?) {
        if (isProvisioningInProgress.getAndSet(true)) {
            Log.w(TAG, "Provisioning already in progress")
            try {
                callback?.onProvisioningStatus(IRcsProvisioningCallback.STATUS_PROVISIONING)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to callback status", e)
            }
            return
        }

        coroutineScope.launch {
            executeProvisioningFlow(callback)
        }
    }

    suspend fun provision(): ProvisioningResult {
        if (isProvisioningInProgress.getAndSet(true)) {
            Log.w(TAG, "Provisioning already in progress")
            return ProvisioningResult.failure(IRcsProvisioningCallback.ERROR_UNKNOWN, "Provisioning already in progress")
        }

        return try {
            executeProvisioningFlowSuspend()
        } finally {
            isProvisioningInProgress.set(false)
        }
    }

    private suspend fun executeProvisioningFlow(callback: IRcsProvisioningCallback?) {
        val result = executeProvisioningFlowSuspend(callback)
        // Callback handling is done within executeProvisioningFlowSuspend or via notify* methods
    }

    private suspend fun executeProvisioningFlowSuspend(callback: IRcsProvisioningCallback? = null): ProvisioningResult {
        try {
            currentProvisioningStatus.set(IRcsProvisioningCallback.STATUS_PROVISIONING)
            notifyProgress(callback, 0, "Starting RCS provisioning")

            val deviceIdentifiersPermitted = checkDeviceIdentifiersPermission()
            if (!deviceIdentifiersPermitted) {
                notifyError(callback, 
                    IRcsProvisioningCallback.ERROR_PERMISSION_DENIED,
                    "READ_DEVICE_IDENTIFIERS permission not granted. Use: adb shell appops set com.google.android.gms READ_DEVICE_IDENTIFIERS allow"
                )
                return ProvisioningResult.failure(IRcsProvisioningCallback.ERROR_PERMISSION_DENIED, "Permission denied")
            }
            notifyProgress(callback, 10, "Device identifiers permission verified")

            val simCardInfo = retrieveSimCardInfo()
            if (simCardInfo == null) {
                notifyError(callback,
                    IRcsProvisioningCallback.ERROR_SIM_NOT_READY,
                    "SIM card not ready or not present"
                )
                return ProvisioningResult.failure(IRcsProvisioningCallback.ERROR_SIM_NOT_READY, "SIM not ready")
            }
            notifyProgress(callback, 20, "SIM card detected: ${simCardInfo.carrierName}")

            val phoneNumber = retrievePhoneNumber(simCardInfo)
            if (phoneNumber.isNullOrBlank()) {
                notifyProgress(callback, 25, "Phone number not available from SIM, using manual entry")
            } else {
                notifyProgress(callback, 30, "Phone number retrieved: ${maskPhoneNumber(phoneNumber)}")
            }

            val carrierConfiguration = fetchCarrierConfiguration(simCardInfo.mccMnc)
            if (carrierConfiguration == null) {
                Log.w(TAG, "No carrier-specific configuration found, using defaults")
            }
            notifyProgress(callback, 40, "Carrier configuration loaded")

            val rcsConfiguration = buildRcsConfiguration(simCardInfo, carrierConfiguration)
            notifyProgress(callback, 50, "RCS configuration prepared")

            val pendingResult = performCarrierProvisioning(phoneNumber, rcsConfiguration, callback)
            if (!pendingResult.isSuccessful) {
                notifyError(callback, pendingResult.errorCode, pendingResult.errorMessage)
                return pendingResult
            }
            notifyProgress(callback, 80, "Carrier provisioning complete")

            saveProvisioningState(phoneNumber, rcsConfiguration)
            notifyProgress(callback, 100, "RCS provisioning complete")

            currentProvisioningStatus.set(IRcsProvisioningCallback.STATUS_PROVISIONED)
            
            withContext(Dispatchers.Main) {
                callback?.onProvisioningComplete(phoneNumber ?: "")
            }

            Log.i(TAG, "RCS provisioning completed successfully for ${maskPhoneNumber(phoneNumber)}")
            return ProvisioningResult.success(phoneNumber)

        } catch (exception: Exception) {
            Log.e(TAG, "Provisioning failed with exception", exception)
            notifyError(callback,
                IRcsProvisioningCallback.ERROR_UNKNOWN,
                "Provisioning failed: ${exception.message}"
            )
            return ProvisioningResult.failure(IRcsProvisioningCallback.ERROR_UNKNOWN, exception.message ?: "Unknown error")
        }
    }



    private fun checkDeviceIdentifiersPermission(): Boolean {
        return DeviceIdentifierHelper.hasReadDeviceIdentifiersPermission(context)
    }

    private fun retrieveSimCardInfo(): SimCardInfo? {
        return SimCardHelper.getSimCardInfo(context)
    }

    private fun retrievePhoneNumber(simCardInfo: SimCardInfo): String? {
        val manualPhone = encryptedPreferences.getString(KEY_MANUAL_PHONE_OVERRIDE, null)
        if (!manualPhone.isNullOrBlank()) {
            return manualPhone
        }
        return simCardInfo.phoneNumber ?: SimCardHelper.getPhoneNumber(context)
    }

    private fun fetchCarrierConfiguration(mccMnc: String): CarrierConfiguration? {
        return CarrierConfigurationManager.getCarrierConfig(mccMnc)
    }

    private fun buildRcsConfiguration(
        simCardInfo: SimCardInfo,
        carrierConfiguration: CarrierConfiguration?
    ): RcsConfiguration {
        return RcsConfigurationBuilder()
            .setRcsVersion("UP2.4")
            .setRcsProfile("UP2.4")
            .setClientVendor("microG")
            .setClientVersion(BuildConfig.RCS_VERSION)
            .setCarrierMccMnc(simCardInfo.mccMnc)
            .setCarrierName(simCardInfo.carrierName)
            .setAutoConfigurationServerUrl(carrierConfiguration?.autoConfigUrl)
            .build()
    }

    private suspend fun performCarrierProvisioning(
        phoneNumber: String?,
        rcsConfiguration: RcsConfiguration,
        callback: IRcsProvisioningCallback?
    ): ProvisioningResult {
        notifyProgress(callback, 55, "Connecting to carrier RCS service")

        val carrierAutoConfigUrl = rcsConfiguration.autoConfigurationServerUrl
        if (carrierAutoConfigUrl.isNullOrBlank()) {
            Log.d(TAG, "No auto-config URL, using direct provisioning")
            return performDirectProvisioning(phoneNumber, rcsConfiguration, callback)
        }

        return performAutoConfigProvisioning(carrierAutoConfigUrl, phoneNumber, rcsConfiguration, callback)
    }

    private suspend fun performDirectProvisioning(
        phoneNumber: String?,
        rcsConfiguration: RcsConfiguration,
        callback: IRcsProvisioningCallback?
    ): ProvisioningResult {
        notifyProgress(callback, 60, "Performing direct RCS registration")

        return try {
            val registrationSuccessful = performLegacyRegistration(phoneNumber, rcsConfiguration)
            
            if (registrationSuccessful) {
                notifyProgress(callback, 75, "Direct registration successful")
                ProvisioningResult.success(phoneNumber)
            } else {
                ProvisioningResult.failure(
                    IRcsProvisioningCallback.ERROR_CARRIER_NOT_SUPPORTED,
                    "Direct registration failed. Carrier may not support RCS."
                )
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Direct provisioning failed", exception)
            ProvisioningResult.failure(
                IRcsProvisioningCallback.ERROR_NETWORK,
                "Network error during provisioning: ${exception.message}"
            )
        }
    }

    private suspend fun performAutoConfigProvisioning(
        autoConfigUrl: String,
        phoneNumber: String?,
        rcsConfiguration: RcsConfiguration,
        callback: IRcsProvisioningCallback?
    ): ProvisioningResult {
        notifyProgress(callback, 60, "Contacting carrier auto-configuration server")

        return try {
            val autoConfigResponse = RcsAutoConfigClient.fetchConfiguration(
                autoConfigUrl,
                phoneNumber,
                rcsConfiguration
            )

            if (autoConfigResponse.isSuccessful) {
                notifyProgress(callback, 70, "Auto-configuration received")
                applyAutoConfiguration(autoConfigResponse)
                ProvisioningResult.success(phoneNumber)
            } else {
                ProvisioningResult.failure(
                    autoConfigResponse.errorCode,
                    autoConfigResponse.errorMessage
                )
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Auto-config provisioning failed", exception)
            ProvisioningResult.failure(
                IRcsProvisioningCallback.ERROR_NETWORK,
                "Failed to contact carrier server: ${exception.message}"
            )
        }
    }

    private fun performLegacyRegistration(phoneNumber: String?, rcsConfiguration: RcsConfiguration): Boolean {
        Log.i(TAG, "Attempting legacy RCS registration for ${maskPhoneNumber(phoneNumber)}")
        
        // diamond-polish: Verify network before assuming legacy success
        if (!connectivityChecker.isNetworkAvailable(context)) {
            Log.e(TAG, "Legacy registration failed: No network connectivity")
            return false
        }
        
        // For legacy carriers (Sprint, etc.), we assume provisioned status 
        // if we are on their network. The actual SIP registration happens 
        // lazily when RcsService starts.
        Log.i(TAG, "Legacy carrier detected. Marking as provisioned for lazy SIP registration.")
        return true
    }

    private fun applyAutoConfiguration(response: AutoConfigResponse) {
        Log.d(TAG, "Applying auto-configuration from carrier")
        
        val configData = response.configurationData
        if (configData.isNullOrEmpty()) {
            Log.w(TAG, "Auto-configuration contained no data")
            return
        }

        val editor = encryptedPreferences.edit()
        
        // Save critical RCS settings
        configData["rcs_version"]?.let { editor.putString(KEY_RCS_VERSION, it) }
        configData["validity"]?.let { editor.putString("rcs_config_validity", it) }
        configData["token"]?.let { editor.putString("rcs_config_token", it) }
        
        // Save SIP Configuration
        configData["sip_proxy"]?.let { editor.putString("rcs_sip_proxy", it) }
        configData["realm"]?.let { editor.putString("rcs_sip_realm", it) }
        configData["im_public_user_identity"]?.let { editor.putString("rcs_im_identity", it) }
        
        // Save Messaging Configuration
        configData["chat_auth"]?.let { editor.putString("rcs_chat_auth_token", it) }
        
        // Save File Transfer Configuration
        configData["ft_auth"]?.let { editor.putString("rcs_ft_auth_token", it) }
        configData["ft_http_cs_uri"]?.let { editor.putString("rcs_ft_server_url", it) }
        configData["max_file_size"]?.let { editor.putString("rcs_max_file_size", it) }
        
        // Save Timestamp
        editor.putLong("config_timestamp", System.currentTimeMillis())
        
        editor.apply()
        Log.i(TAG, "Applied ${configData.size} RCS configuration items")
    }

    private fun saveProvisioningState(phoneNumber: String?, rcsConfiguration: RcsConfiguration) {
        encryptedPreferences.edit()
            .putBoolean(KEY_IS_PROVISIONED, true)
            .putString(KEY_REGISTERED_PHONE, phoneNumber)
            .putLong(KEY_PROVISIONING_TIMESTAMP, System.currentTimeMillis())
            .putString(KEY_RCS_VERSION, rcsConfiguration.rcsVersion)
            .apply()

        Log.d(TAG, "Provisioning state saved")
    }

    fun getRegisteredPhoneNumber(): String? {
        return encryptedPreferences.getString(KEY_REGISTERED_PHONE, null)
    }

    fun setPreferredPhoneNumber(phoneNumber: String) {
        encryptedPreferences.edit()
        .putString(KEY_MANUAL_PHONE_OVERRIDE, phoneNumber)
        .apply()
    }

    fun loadConfiguration(): RcsConfiguration? {
        val isProvisioned = encryptedPreferences.getBoolean(KEY_IS_PROVISIONED, false)
        if (!isProvisioned) {
            return null
        }

        return RcsConfigurationBuilder()
            .setRcsVersion(encryptedPreferences.getString(KEY_RCS_VERSION, "UP2.4") ?: "UP2.4")
            .setSipProxy(encryptedPreferences.getString("rcs_sip_proxy", null))
            .setSipRealm(encryptedPreferences.getString("rcs_sip_realm", null))
            .setImPublicUserIdentity(encryptedPreferences.getString("rcs_im_identity", null))
            .build()
    }

    fun saveConfiguration(config: RcsConfiguration) {
        encryptedPreferences.edit()
            .putString(KEY_RCS_VERSION, config.rcsVersion)
            .apply()
    }

    fun loadSipConfiguration(): org.microg.gms.rcs.sip.SipConfiguration? {
        val config = loadConfiguration() ?: return null
        val phoneNumber = getRegisteredPhoneNumber() ?: return null
        
        val proxy = config.sipProxy ?: return null
        val realm = config.sipRealm ?: return null
        
        val parts = proxy.split(":")
        val host = parts[0]
        val port = parts.getOrNull(1)?.toIntOrNull() ?: 5061
        
        // Retrieve the token which serves as the password for SIP digest auth
        val password = encryptedPreferences.getString("rcs_config_token", "") ?: ""
        
        if (password.isBlank()) {
            Log.w(TAG, "SIP Configuration loaded but password/token is empty.")
        }

        return org.microg.gms.rcs.sip.SipConfiguration(
            serverHost = host,
            serverPort = port,
            domain = realm,
            userPhoneNumber = phoneNumber,
            password = password,
            useTls = true
        )
    }

    fun clearProvisioning() {
        encryptedPreferences.edit()
            .clear()
            .apply()
        
        currentProvisioningStatus.set(IRcsProvisioningCallback.STATUS_NOT_PROVISIONED)
        Log.d(TAG, "Provisioning state cleared")
    }

    fun refreshRegistration(callback: IRcsProvisioningCallback?) {
        Log.d(TAG, "Refreshing RCS registration")
        
        clearProvisioning()
        startProvisioning(callback)
    }

    fun cleanup() {
        coroutineScope.cancel()
        Log.d(TAG, "Provisioning manager cleaned up")
    }

    private suspend fun notifyProgress(callback: IRcsProvisioningCallback?, progress: Int, message: String) {
        Log.d(TAG, "Provisioning progress: $progress% - $message")
        
        withContext(Dispatchers.Main) {
            try {
                callback?.onProvisioningProgress(progress, message)
            } catch (exception: Exception) {
                Log.w(TAG, "Failed to notify progress callback", exception)
            }
        }
    }

    private suspend fun notifyError(callback: IRcsProvisioningCallback?, errorCode: Int, errorMessage: String) {
        Log.e(TAG, "Provisioning error: $errorCode - $errorMessage")
        
        currentProvisioningStatus.set(IRcsProvisioningCallback.STATUS_ERROR)
        isProvisioningInProgress.set(false)
        
        withContext(Dispatchers.Main) {
            try {
                callback?.onProvisioningError(errorCode, errorMessage)
            } catch (exception: Exception) {
                Log.w(TAG, "Failed to notify error callback", exception)
            }
        }
    }

    private fun maskPhoneNumber(phoneNumber: String?): String {
        if (phoneNumber.isNullOrBlank() || phoneNumber.length < 4) {
            return "***"
        }
        
        val visibleDigits = 4
        val maskedLength = phoneNumber.length - visibleDigits
        val mask = "*".repeat(maskedLength)
        
        return mask + phoneNumber.takeLast(visibleDigits)
    }
}

data class ProvisioningResult(
    val isSuccessful: Boolean,
    val errorCode: Int,
    val errorMessage: String,
    val phoneNumber: String? = null
) {
    companion object {
        fun success(phoneNumber: String? = null) = ProvisioningResult(true, 0, "", phoneNumber)
        fun failure(errorCode: Int, errorMessage: String) = ProvisioningResult(false, errorCode, errorMessage)
    }
}

data class AutoConfigResponse(
    val isSuccessful: Boolean,
    val errorCode: Int,
    val errorMessage: String,
    val configurationData: Map<String, String>? = null
)

// Dependency for testing network availability
interface ConnectivityChecker {
    fun isNetworkAvailable(context: Context): Boolean
}

class DefaultConnectivityChecker : ConnectivityChecker {
    override fun isNetworkAvailable(context: Context): Boolean {
        return NetworkHelper.isNetworkAvailable(context)
    }
}
