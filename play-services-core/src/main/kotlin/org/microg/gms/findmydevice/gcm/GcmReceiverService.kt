/**
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.findmydevice.gcm

import android.accounts.Account
import android.accounts.AccountManager
import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.telephony.TelephonyManager
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.legacy.content.WakefulBroadcastReceiver
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.runBlocking
import org.microg.gms.auth.AuthConstants
import org.microg.gms.auth.AuthManager
import org.microg.gms.auth.AuthPrefs
import org.microg.gms.checkin.LastCheckinInfo
import org.microg.gms.common.Constants
import org.microg.gms.common.ForegroundServiceContext
import org.microg.gms.findmydevice.AlarmRingService
import org.microg.gms.findmydevice.FIND_DEVICE_REMOTE_POLICY
import org.microg.gms.findmydevice.FMD_BASE_URL
import org.microg.gms.findmydevice.GMS_FMD_OAUTH_SERVICE
import org.microg.gms.findmydevice.ProcessSitrepInterceptor
import org.microg.gms.findmydevice.RemotePayloadInterceptor
import org.microg.gms.findmydevice.TAG
import org.microg.gms.fmd.BatteryStatus
import org.microg.gms.fmd.ConnectivityStatus
import org.microg.gms.fmd.DeviceAdminStatus
import org.microg.gms.fmd.FmdApiServiceClient
import org.microg.gms.fmd.Location
import org.microg.gms.fmd.PasswordRequirements
import org.microg.gms.fmd.RemotePayloadRequest
import org.microg.gms.fmd.RemotePolicy
import org.microg.gms.fmd.SitrepRequest
import org.microg.gms.gcm.GcmConstants
import org.microg.gms.gcm.KEY_GCM_REG_ID
import org.microg.gms.gcm.ReceiverService
import org.microg.gms.gcm.createGrpcClient
import org.microg.gms.profile.Build
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class GcmReceiver : WakefulBroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val allowedFindDevices = AuthPrefs.allowedFindDevices(context)
        if (!allowedFindDevices) {
            Log.d(TAG, "onReceive: <Find-Device> Switch not allowed ")
            return
        }
        Log.d(TAG, "onReceive: action: ${intent.action}")
        val callIntent = Intent(context, GcmReceiverService::class.java)
        callIntent.action = intent.action
        intent.extras?.let { callIntent.putExtras(it) }
        ForegroundServiceContext(context).startService(callIntent)
    }
}

class GcmReceiverService : ReceiverService(TAG) {

    private enum class PolicyAction(val action: Int) {
        ACTION_RESET(1), ACTION_REPORT(2), ACTION_RING_START(3), ACTION_LOCK(5), ACTION_FIND(6), ACTION_RING_STOP(10)
    }

    override fun allowed(): Boolean = AuthPrefs.allowedFindDevices(this)

    override fun receiver(intent: Intent) {
        Log.d(TAG, "receiver: run thread -> ${Thread.currentThread().name}")
        Log.d(TAG, "receiver: action: ${intent.action} data: ${intent.extras}")
        val data = intent.extras ?: return run { Log.w(TAG, "receiver: intent.extras is null") }
        val accountManager = getSystemService(ACCOUNT_SERVICE) as AccountManager? ?: return run { Log.w(TAG, "receiver: accountManager is null") }
        val gcmType = data.getString(GcmConstants.EXTRA_GCM_TYPE)
        if (gcmType == null || gcmType != FIND_DEVICE_REMOTE_POLICY) {
            Log.w(TAG, "receiver: gcmType not matched, type: $gcmType")
            return
        }
        val remotePolicyData = data.getString(GcmConstants.EXTRA_GCM_RP)
        val gcmPayload = data.getString(GcmConstants.EXTRA_GCM_PAYLOAD)
        val policyData = if (!remotePolicyData.isNullOrEmpty()) remotePolicyData else gcmPayload
        if (policyData.isNullOrEmpty()) {
            Log.w(TAG, "No policy data available")
            return
        }
        val remotePolicy = runCatching {
            Base64.decode(policyData, Base64.DEFAULT)?.let { RemotePolicy.ADAPTER.decode(it) }
        }.getOrNull() ?: return run { Log.w(TAG, "Invalid policy data") }
        Log.d(TAG, "RemotePolicy parsed successfully: $remotePolicy")
        val account = remotePolicy.email_hash?.let { emailHash ->
            accountManager.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE).find {
                val digest = MessageDigest.getInstance("SHA-256").digest(it.name.lowercase().toByteArray(Charsets.UTF_8))
                digest.joinToString("") { c -> "%02x".format(c) } == emailHash.hex()
            }
        } ?: return run { Log.w(TAG, "receiver: account not matched!") }
        Log.d(TAG, "receiver: account: ${account.name}")
        handleRemotePolicyAction(remotePolicy, account)
    }

    private fun handleRemotePolicyAction(policy: RemotePolicy, account: Account) {
        Log.d(TAG, "handleRemotePolicyAction: action=${policy.action}, account=${account.name}")
        val policyToken = policy.token ?: return run { Log.w(TAG, "handleRemotePolicyAction: policyToken is null!") }
        Log.d(TAG, "handleRemotePolicyAction: policyToken: $policyToken")
        when (policy.action) {
            PolicyAction.ACTION_RESET.action -> {
                Log.d(TAG, "handleRemotePolicyAction: Reset device.")
            }

            PolicyAction.ACTION_REPORT.action -> {
                Log.d(TAG, "handleRemotePolicyAction: Report device information.")
                uploadRemotePayload(
                    policyToken,
                    uploadLocation = true,
                    uploadRequirements = true,
                    uploadBatteryInfo = policy.include_battery_status == true,
                    uploadConnectionInfo = policy.include_connectivity_status == true
                )
            }

            PolicyAction.ACTION_RING_START.action -> {
                Log.d(TAG, "handleRemotePolicyAction: Start ring device.")
                uploadRemotePayload(
                    policyToken,
                    uploadBatteryInfo = policy.include_battery_status == true,
                    uploadConnectionInfo = policy.include_connectivity_status == true
                )
                AlarmRingService.startRing(this)
            }

            PolicyAction.ACTION_LOCK.action -> {
                Log.d(TAG, "handleRemotePolicyAction: Lock device.")
            }

            PolicyAction.ACTION_FIND.action -> {
                Log.d(TAG, "handleRemotePolicyAction: Find device.")
                uploadRemotePayload(
                    policyToken,
                    uploadBatteryInfo = policy.include_battery_status == true,
                    uploadConnectionInfo = policy.include_connectivity_status == true
                )
                uploadProcessSitrep(account)
            }

            PolicyAction.ACTION_RING_STOP.action -> {
                Log.d(TAG, "handleRemotePolicyAction: Stop ring device.")
                uploadRemotePayload(
                    policyToken,
                    uploadBatteryInfo = policy.include_battery_status == true,
                    uploadConnectionInfo = policy.include_connectivity_status == true
                )
                AlarmRingService.stopRing(this)
            }

            else -> {
                Log.d(TAG, "handleRemotePolicyAction: Unknown action: ${policy.action}")
            }
        }
    }

    private fun uploadRemotePayload(
        policyToken: String,
        uploadLocation: Boolean = false,
        uploadBatteryInfo: Boolean = false,
        uploadConnectionInfo: Boolean = false,
        uploadRequirements: Boolean = false,
    ) {
        fun loadLocation(): Location {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            val location = Tasks.await(fusedLocationClient.lastLocation, 5, TimeUnit.SECONDS)
            return Location(latitude = location.latitude, longitude = location.longitude, accuracy = location.accuracy, timestamp = location.time)
        }

        fun loadBatteryInfo(): BatteryStatus {
            val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val charging = if (batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0) 1 else 0
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN) ?: BatteryManager.BATTERY_STATUS_UNKNOWN
            val health = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN) ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
            val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
            return BatteryStatus(charging, level, scale, status, health, plugged)
        }

        @RequiresApi(23)
        fun loadConnectionInfo(): ConnectivityStatus {
            val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
            val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            val isMobile = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
            val signalLevel = if (isWifi) {
                (WifiManager.calculateSignalLevel(wifiManager.connectionInfo.rssi, 4) + 1).coerceIn(1, 4)
            } else if (isMobile && Build.VERSION.SDK_INT >= 28) {
                telephonyManager.signalStrength?.let { (it.level + 1).coerceIn(1, 4) } ?: 2
            } else 2
            val carrierName = telephonyManager.simOperatorName.takeIf { it.isNotEmpty() } ?: "unknown"
            val networkName = if (isWifi) {
                wifiManager.connectionInfo.ssid?.takeIf { it != "<unknown ssid>" }?.replace("\"", "") ?: "WiFi"
            } else ""
            return ConnectivityStatus(
                is_connected = if (isWifi) 1 else if (isMobile) 2 else 0,
                type_name = networkName,
                type = signalLevel,
                subtype = signalLevel,
                extra_info = carrierName
            )
        }

        fun loadPasswordRequirements(): PasswordRequirements {
            return PasswordRequirements(
                min_length = 0, min_letters = 16, min_lowercase = 0, min_uppercase = 0, min_numeric = 0,
                min_symbols = 0, min_non_letter = 0, max_failed_attempts = 0, password_quality = 0
            )
        }

        fun isDeviceLocked(): Boolean {
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            return keyguardManager.isKeyguardLocked
        }

        val api = createGrpcClient<FmdApiServiceClient>(
            baseUrl = FMD_BASE_URL, interceptor = RemotePayloadInterceptor()
        )
        api.RemotePayload().executeBlocking(
            RemotePayloadRequest(
                token = policyToken,
                response_codes = listOf(0),
                location = if (uploadLocation) loadLocation() else null,
                battery_status = if (uploadBatteryInfo) loadBatteryInfo() else null,
                connectivity_status = if (uploadConnectionInfo) loadConnectionInfo() else null,
                password_requirements = if (uploadRequirements) loadPasswordRequirements() else null,
                has_lock_screen = isDeviceLocked()
            )
        )
    }

    fun uploadProcessSitrep(account: Account) {
        val regId = getSharedPreferences("com.google.android.gcm", MODE_PRIVATE)?.getString(KEY_GCM_REG_ID, null)
        if (regId == null) {
            Log.d(TAG, "uploadProcessSitrep: regId is null!")
            return
        }
        Log.d(TAG, "uploadProcessSitrep: regId: $regId")
        val androidId = LastCheckinInfo.read(this).androidId
        if (androidId == 0L) {
            Log.w(TAG, "Device not checked in, cannot upload!")
            return
        }
        val deviceDataVersionInfo = LastCheckinInfo.read(this).deviceDataVersionInfo
        Log.d(TAG, "uploadProcessSitrep: deviceDataVersion=$deviceDataVersionInfo")
        val oauthToken = runBlocking {
            val authManager = AuthManager(this@GcmReceiverService, account.name, Constants.GMS_PACKAGE_NAME, GMS_FMD_OAUTH_SERVICE)
            runCatching { authManager.requestAuth(true).auth }.getOrNull()
        }
        if (oauthToken.isNullOrEmpty()) {
            Log.w(TAG, "oauthToken get error!")
            return
        }

        fun isLockscreenEnabled(): Boolean {
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            return if (Build.VERSION.SDK_INT >= 23) {
                keyguardManager.isDeviceSecure
            } else {
                @Suppress("DEPRECATION")
                keyguardManager.isKeyguardSecure
            }
        }

        fun getPhoneType(): Int {
            val telephonyManager = getSystemService(TELEPHONY_SERVICE) as? TelephonyManager
            return telephonyManager?.phoneType ?: TelephonyManager.PHONE_TYPE_NONE
        }

        fun getDeviceAdminStatus(): DeviceAdminStatus {
            val devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val hasDeviceOwner = if (Build.VERSION.SDK_INT >= 21) {
                devicePolicyManager.isDeviceOwnerApp(packageName)
            } else {
                false
            }
            val hasProfileOwner = if (Build.VERSION.SDK_INT >= 21) {
                devicePolicyManager.isProfileOwnerApp(packageName)
            } else {
                false
            }
            return DeviceAdminStatus(
                is_device_admin = false,
                has_device_owner = hasDeviceOwner,
                has_profile_owner = hasProfileOwner
            )
        }

        val api = createGrpcClient<FmdApiServiceClient>(
            baseUrl = FMD_BASE_URL, interceptor = ProcessSitrepInterceptor(oauthToken)
        )
        api.ProcessSitrep().executeBlocking(
            SitrepRequest(
                android_id = androidId,
                gcm_registration_id = regId,
                gms_version = Constants.GMS_VERSION_CODE,
                reason = 8,
                retry_reason = 0,
                sdk_version = Build.VERSION.SDK_INT,
                phone_type = getPhoneType(),
                device_data_version = deviceDataVersionInfo,
                lockscreen_enabled = isLockscreenEnabled(),
                device_admin_status = getDeviceAdminStatus()
            )
        )
    }
}