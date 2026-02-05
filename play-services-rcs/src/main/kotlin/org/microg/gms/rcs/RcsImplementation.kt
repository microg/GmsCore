/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsImplementation - Core RCS service implementation
 * 
 * Implements the IRcsService interface and handles all RCS operations
 * including registration, capabilities exchange, and configuration.
 */

package org.microg.gms.rcs

import android.content.Context
import android.os.RemoteException
import android.util.Log
import com.google.android.gms.rcs.IRcsCapabilitiesCallback
import com.google.android.gms.rcs.IRcsProvisioningCallback
import com.google.android.gms.rcs.IRcsService
import com.google.android.gms.rcs.IRcsServiceCallback
import com.google.android.gms.rcs.IRcsStateListener
import kotlinx.coroutines.launch
import com.google.android.gms.rcs.RcsCapabilities
import com.google.android.gms.rcs.RcsConfiguration
import java.util.concurrent.CopyOnWriteArrayList
import org.microg.gms.rcs.sip.RcsSipClient
import org.microg.gms.rcs.sip.SipConfiguration
import org.microg.gms.rcs.sip.SipMessageResult
import org.microg.gms.rcs.sip.SipErrorCode

class RcsImplementation(
    private val context: Context,
    private val provisioningManager: RcsProvisioningManager,
    private val capabilitiesManager: RcsCapabilitiesManager
) : IRcsService.Stub() {

    companion object {
        private const val TAG = "RcsImplementation"
        private const val RCS_SERVICE_VERSION = 1
    }

    private val stateListeners = CopyOnWriteArrayList<IRcsStateListener>()
    private var currentConfiguration: RcsConfiguration? = null
    private var sipClient: RcsSipClient? = null

    fun initialize() {
        initializeSip()
    }

    private fun initializeSip() {
        val sipConfig = provisioningManager.loadSipConfiguration()
        if (sipConfig == null) {
             Log.w(TAG, "Cannot initialize SIP: No valid configuration found")
             return
        }

        Log.d(TAG, "Initializing SIP client with config: $sipConfig")
        
        sipClient = RcsSipClient(context, sipConfig)
        
        // Connect in background
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            if (sipClient?.connect() == true) {
                 val phoneNumber = sipConfig.userPhoneNumber
                 sipClient?.register(phoneNumber, "unknown_imei") // TODO: Get IMEI
            }
        }
    }

    fun sendMessage(targetUri: String, content: String): SipMessageResult {
        // Fetch the active SIP client from the ServiceLocator (managed by Orchestrator)
        val client = org.microg.gms.rcs.di.RcsServiceLocator.getSipClient()
        
        if (client == null) {
             return SipMessageResult(false, errorCode = SipErrorCode.NOT_REGISTERED, errorMessage = "RCS not configured or registered")
        }
        
        return client.sendMessage(targetUri, content)
    }

    override fun getVersion(): Int {
        return RCS_SERVICE_VERSION
    }

    override fun isRcsAvailable(): Boolean {
        val deviceIdentifiersAllowed = DeviceIdentifierHelper.hasReadDeviceIdentifiersPermission(context)
        val simReady = SimCardHelper.isSimCardReady(context)
        val networkAvailable = NetworkHelper.isNetworkAvailable(context)
        
        val isAvailable = deviceIdentifiersAllowed && simReady && networkAvailable
        
        Log.d(TAG, "RCS availability check: " +
                "deviceIdentifiers=$deviceIdentifiersAllowed, " +
                "simReady=$simReady, " +
                "networkAvailable=$networkAvailable, " +
                "result=$isAvailable")
        
        return isAvailable
    }

    override fun isRcsEnabled(): Boolean {
        return provisioningManager.isProvisioned()
    }

    override fun enableRcs(callback: IRcsProvisioningCallback?) {
        Log.d(TAG, "Enable RCS requested")
        
        if (!isRcsAvailable()) {
            callback?.onProvisioningError(
                IRcsProvisioningCallback.ERROR_PERMISSION_DENIED,
                "RCS is not available on this device. Check permissions and SIM card."
            )
            return
        }
        
        provisioningManager.startProvisioning(callback)
    }

    override fun disableRcs(callback: IRcsServiceCallback?) {
        Log.d(TAG, "Disable RCS requested")
        
        provisioningManager.clearProvisioning()
        notifyStateChanged(IRcsStateListener.STATE_DISABLED)
        
        callback?.onSuccess()
    }

    override fun getProvisioningStatus(callback: IRcsProvisioningCallback?) {
        val status = provisioningManager.getProvisioningStatus()
        callback?.onProvisioningStatus(status)
    }

    override fun startProvisioning(callback: IRcsProvisioningCallback?) {
        Log.d(TAG, "Starting RCS provisioning")
        
        if (!isRcsAvailable()) {
            val errorMessage = buildUnavailabilityReason()
            callback?.onProvisioningError(
                IRcsProvisioningCallback.ERROR_PERMISSION_DENIED,
                errorMessage
            )
            return
        }
        
        provisioningManager.startProvisioning(callback)
    }

    private fun buildUnavailabilityReason(): String {
        val reasons = mutableListOf<String>()
        
        if (!DeviceIdentifierHelper.hasReadDeviceIdentifiersPermission(context)) {
            reasons.add("READ_DEVICE_IDENTIFIERS permission not granted")
        }
        if (!SimCardHelper.isSimCardReady(context)) {
            reasons.add("SIM card not ready")
        }
        if (!NetworkHelper.isNetworkAvailable(context)) {
            reasons.add("No network connection")
        }
        
        return if (reasons.isEmpty()) {
            "Unknown error"
        } else {
            reasons.joinToString("; ")
        }
    }

    override fun getCapabilities(phoneNumber: String?, callback: IRcsCapabilitiesCallback?) {
        if (phoneNumber.isNullOrBlank()) {
            callback?.onError(phoneNumber ?: "", 
                IRcsProvisioningCallback.ERROR_UNKNOWN, 
                "Phone number is required")
            return
        }
        
        capabilitiesManager.queryCapabilities(phoneNumber, callback)
    }

    override fun getCapabilitiesBulk(phoneNumbers: MutableList<String>?, callback: IRcsCapabilitiesCallback?) {
        if (phoneNumbers.isNullOrEmpty()) {
            callback?.onError("", 
                IRcsProvisioningCallback.ERROR_UNKNOWN, 
                "Phone numbers list is required")
            return
        }
        
        capabilitiesManager.queryCapabilitiesBulk(phoneNumbers, callback)
    }

    override fun publishCapabilities(capabilitiesMask: Int, callback: IRcsServiceCallback?) {
        Log.d(TAG, "Publishing capabilities: $capabilitiesMask")
        
        capabilitiesManager.publishOwnCapabilities(capabilitiesMask)
        callback?.onSuccess()
    }

    override fun getConfiguration(): RcsConfiguration? {
        return currentConfiguration ?: provisioningManager.loadConfiguration()
    }

    override fun updateConfiguration(config: RcsConfiguration?, callback: IRcsServiceCallback?) {
        if (config == null) {
            callback?.onError(
                IRcsProvisioningCallback.ERROR_UNKNOWN,
                "Configuration cannot be null"
            )
            return
        }
        
        currentConfiguration = config
        provisioningManager.saveConfiguration(config)
        callback?.onSuccess()
    }

    override fun registerRcsStateListener(listener: IRcsStateListener?) {
        if (listener != null && !stateListeners.contains(listener)) {
            stateListeners.add(listener)
            Log.d(TAG, "State listener registered. Total listeners: ${stateListeners.size}")
        }
    }

    override fun unregisterRcsStateListener(listener: IRcsStateListener?) {
        if (listener != null) {
            stateListeners.remove(listener)
            Log.d(TAG, "State listener unregistered. Total listeners: ${stateListeners.size}")
        }
    }

    override fun getRegisteredPhoneNumber(): String? {
        return provisioningManager.getRegisteredPhoneNumber()
    }

    override fun setPreferredPhoneNumber(phoneNumber: String?, callback: IRcsServiceCallback?) {
        if (phoneNumber.isNullOrBlank()) {
            callback?.onError(
                IRcsProvisioningCallback.ERROR_UNKNOWN,
                "Phone number cannot be empty"
            )
            return
        }
        
        provisioningManager.setPreferredPhoneNumber(phoneNumber)
        callback?.onSuccess()
    }

    override fun refreshRegistration(callback: IRcsProvisioningCallback?) {
        Log.d(TAG, "Refreshing RCS registration")
        
        provisioningManager.refreshRegistration(callback)
    }

    override fun getCarrierConfiguration(mccMnc: String?): RcsConfiguration? {
        if (mccMnc.isNullOrBlank()) {
            return null
        }
        
        return CarrierConfigurationManager.getConfiguration(mccMnc)
    }

    override fun isDeviceTrusted(): Boolean {
        return DeviceIntegrityChecker.isDeviceTrusted(context)
    }

    private fun notifyStateChanged(newState: Int) {
        val deadListeners = mutableListOf<IRcsStateListener>()
        
        for (listener in stateListeners) {
            try {
                listener.onRcsStateChanged(newState)
            } catch (exception: RemoteException) {
                Log.w(TAG, "Listener is dead, removing from list")
                deadListeners.add(listener)
            }
        }
        
        stateListeners.removeAll(deadListeners)
    }

    fun notifyRegistrationStateChanged(isRegistered: Boolean, phoneNumber: String?) {
        val deadListeners = mutableListOf<IRcsStateListener>()
        
        for (listener in stateListeners) {
            try {
                listener.onRegistrationStateChanged(isRegistered, phoneNumber)
            } catch (exception: RemoteException) {
                Log.w(TAG, "Listener is dead, removing from list")
                deadListeners.add(listener)
            }
        }
        
        stateListeners.removeAll(deadListeners)
    }
}
