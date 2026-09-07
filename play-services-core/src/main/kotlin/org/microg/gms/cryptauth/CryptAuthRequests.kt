/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cryptauth

import android.annotation.SuppressLint
import android.app.Activity
import android.app.KeyguardManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.fingerprint.FingerprintManager
import android.net.ConnectivityManager
import android.provider.Settings
import android.util.Log
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.BuildConfig
import com.google.android.gms.droidguard.DroidGuardClient
import com.google.android.gms.tasks.await
import cryptauthv2.ApplicationSpecificMetadata
import cryptauthv2.ClientAppMetadata
import cryptauthv2.ClientMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.toByteString
import org.json.JSONArray
import org.json.JSONObject
import org.microg.gms.common.Constants
import org.microg.gms.common.Utils
import org.microg.gms.profile.Build
import org.microg.gms.smartdevice.d2d.AuthZenWire
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.sqrt

private const val TAG = "CryptAuthRequests"

private const val CRYPTAUTH_BASE_URL = "https://cryptauthenrollment.googleapis.com/"
private const val CRYPTAUTH_METHOD_SYNC_KEYS = "v1:syncKeys"
private const val CRYPTAUTH_METHOD_ENROLL_KEYS = "v1:enrollKeys"
private const val CRYPTAUTH_NETWORK_TIMEOUT_MILLIS = 30_000

private const val API_KEY = "AIzaSyAP-gfH3qvi6vgHZbSYwQ_XHqV_mXHhzIk"
internal const val CERTIFICATE = "58E1C4133F7441EC3D2C270270A14802DA47BA0E"

internal const val CRYPTAUTH_FIELD_SESSION_ID = "randomSessionId"
private const val ACTION_TRUST_AGENT_STATE_API = "com.google.android.gms.trustagent.StateApi.START"
private const val CRYPTAUTH_DROIDGUARD_USE_CASE = "cryptauth_synckeys_request"
private const val FEATURE_PIXEL_EXPERIENCE = "com.google.android.feature.PIXEL_EXPERIENCE"
private const val FEATURE_ARC = "org.chromium.arc"
private const val SETTING_MANDATORY_BIOMETRICS = "mandatory_biometrics"

internal suspend fun cryptAuthSyncKeys(
    context: Context,
    authToken: String,
    instanceId: String,
    instanceToken: String,
    gcmRegistrationId: String,
    androidId: Long,
    invocationReason: ClientMetadata.InvocationReason,
    keys: List<CryptAuthSyncKeyRequest>,
): JSONObject? {
    // CryptAuth sync request tells server whether or not screenlock is enabled

    val packageManager = context.packageManager
    @Suppress("DEPRECATION")
    val bluetoothAdapter = runCatching { BluetoothAdapter.getDefaultAdapter() }.getOrNull()
    val bluetoothSupported = bluetoothAdapter != null
    val bluetoothEnabled = runCatching { bluetoothAdapter?.isEnabled == true }.getOrDefault(false)
    val bleSupported = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    @Suppress("DEPRECATION")
    val mobileDataSupported = runCatching {
        connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE) != null
    }.getOrDefault(false)
    // microG does not provide tethering hosting, so advertise false.
    val tetheringSupported = false
    val autoUnlockSupported = packageManager.resolveService(
        Intent(ACTION_TRUST_AGENT_STATE_API).setPackage(Constants.GMS_PACKAGE_NAME),
        0,
    ) != null
    val strongBoxSupported = packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
    val biometricUnlockSupported = canAuthenticateWithWeakBiometrics(context)
    val mandatoryBiometricsEnabled = Settings.Secure.getInt(
        context.contentResolver,
        SETTING_MANDATORY_BIOMETRICS,
        0,
    ) != 0
    val displayDiagonalMils = calculateDisplayDiagonalMils(context)
    val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    val locale = if (android.os.Build.VERSION.SDK_INT >= 21) {
        Locale.getDefault().toLanguageTag()
    } else {
        Locale.getDefault().toString()
    }

    // Bind the integrity response to this device and Instance ID.
    val droidGuardResponse = cryptAuthDroidGuardResponse(context, instanceId, androidId)
    val clientAppMetadata = ClientAppMetadata(
        application_specific_metadata = listOf(
            ApplicationSpecificMetadata(
                gcm_registration_id = gcmRegistrationId.toByteArray().toByteString(),
                notification_enabled = notificationsEnabled,
                device_software_version = BuildConfig.VERSION_NAME,
                device_software_version_code = BuildConfig.VERSION_CODE.toLong(),
                device_software_package = Constants.GMS_PACKAGE_NAME
            )
        ),
        instance_id = instanceId,
        instance_id_token = instanceToken,
        android_device_id = androidId,
        locale = locale,
        device_os_version = Build.DISPLAY ?: "",
        device_os_version_code = Build.VERSION.SDK_INT.toLong(),
        device_os_release = Build.VERSION.RELEASE ?: "",
        device_os_codename = Build.VERSION.CODENAME ?: "",
        device_display_diagonal_mils = displayDiagonalMils,
        device_model = Build.MODEL ?: "",
        device_manufacturer = Build.MANUFACTURER ?: "",
        device_type = ClientAppMetadata.DeviceType.ANDROID,
        using_secure_screenlock = context.isLockscreenConfigured(),
        auto_unlock_screenlock_supported = autoUnlockSupported,
        // The local TrustAgent endpoint has no state implementation.
        auto_unlock_screenlock_enabled = false,
        bluetooth_radio_supported = bluetoothSupported,
        bluetooth_radio_enabled = bluetoothEnabled,
        ble_radio_supported = bleSupported,
        mobile_data_supported = mobileDataSupported,
        tethering_supported = tetheringSupported,
        pixel_experience = packageManager.hasSystemFeature(FEATURE_PIXEL_EXPERIENCE),
        arc_plus_plus = packageManager.hasSystemFeature(FEATURE_ARC),
        hardware_user_presence_supported = strongBoxSupported,
        dedicated_secure_element_supported = strongBoxSupported,
        biometricUnlockSupported = biometricUnlockSupported,
        mandatoryBiometricsEnabled = mandatoryBiometricsEnabled,
        droid_guard_response = droidGuardResponse,
    )
        .encodeByteString()
        .base64Url()

    val jsonBody = jsonObjectOf(
        "applicationName" to Constants.GMS_PACKAGE_NAME,
        "clientVersion" to "1.0.0",
        "syncSingleKeyRequests" to JSONArray().apply {
            keys.forEach { key ->
                put(
                    jsonObjectOf(
                        "keyName" to key.name,
                        "keyHandles" to JSONArray().apply {
                            key.handles.forEach { handle -> put(AuthZenWire.base64(handle)) }
                        },
                    ),
                )
            }
        },
        "clientMetadata" to jsonObjectOf(
            "invocationReason" to invocationReason.name,
        ),
        "clientAppMetadata" to clientAppMetadata,
    )

    return cryptAuthQuery(CRYPTAUTH_BASE_URL + CRYPTAUTH_METHOD_SYNC_KEYS, authToken, jsonBody)
}

internal data class CryptAuthSyncKeyRequest(
    val name: String,
    val handles: List<ByteArray>,
)

private suspend fun cryptAuthDroidGuardResponse(context: Context, instanceId: String, androidId: Long): String {
    val inputs = hashMapOf(
        "instanceId" to sha256Hex(instanceId.toByteArray(Charsets.UTF_8)),
        // The service expects the numeric device ID in little-endian order.
        "deviceId" to sha256Hex(
            ByteBuffer.allocate(java.lang.Long.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putLong(androidId)
                .array(),
        ),
    )
    return DroidGuardClient.getResults(context, CRYPTAUTH_DROIDGUARD_USE_CASE, inputs).await()
}

private fun sha256Hex(value: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(value).joinToString(separator = "") { byte ->
        "%02x".format(byte.toInt() and 0xff)
    }

private fun calculateDisplayDiagonalMils(context: Context): Int {
    // Display metrics are meaningful only for a visual Activity context.
    if (context !is Activity) return 0
    @Suppress("DEPRECATION")
    val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
    val metrics = DisplayMetrics()
    @Suppress("DEPRECATION")
    display.getRealMetrics(metrics)
    val heightMils = metrics.heightPixels.toFloat() * 1000.0f / metrics.ydpi
    val widthMils = metrics.widthPixels.toFloat() * 1000.0f / metrics.xdpi
    return sqrt((heightMils * heightMils + widthMils * widthMils).toDouble()).toInt()
}

@SuppressLint("MissingPermission")
private fun canAuthenticateWithWeakBiometrics(context: Context): Boolean = try {
    when {
        android.os.Build.VERSION.SDK_INT >= 30 -> {
            val biometricManager = context.getSystemService(BiometricManager::class.java)
            biometricManager?.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS
        }
        android.os.Build.VERSION.SDK_INT >= 29 -> {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.BIOMETRIC_SERVICE) as? BiometricManager)?.canAuthenticate() ==
                BiometricManager.BIOMETRIC_SUCCESS
        }
        android.os.Build.VERSION.SDK_INT >= 23 -> {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.FINGERPRINT_SERVICE) as? FingerprintManager)?.let {
                it.isHardwareDetected && it.hasEnrolledFingerprints()
            } == true
        }
        else -> false
    }
} catch (e: SecurityException) {
    Log.w(TAG, "Unable to determine biometric support", e)
    false
}

internal suspend fun cryptAuthEnrollKeys(
    authToken: String,
    session: String,
    keys: List<CryptAuthEnrollmentKey>,
    clientEphemeralDh: ByteArray?,
): JSONObject? {
    val jsonBody = JSONObject().apply {
        put(CRYPTAUTH_FIELD_SESSION_ID, session)
        if (clientEphemeralDh != null) {
            put("clientEphemeralDh", AuthZenWire.base64(clientEphemeralDh))
        }
        put(
            "enrollSingleKeyRequests",
            JSONArray().apply {
                keys.forEach { key ->
                    put(
                        jsonObjectOf(
                            "keyName" to key.name,
                            "newKeyHandle" to AuthZenWire.base64(key.handle),
                            "keyMaterial" to AuthZenWire.base64(key.material),
                            "keyProof" to AuthZenWire.base64(key.proof),
                        ),
                    )
                }
            },
        )
    }

    return cryptAuthQuery(CRYPTAUTH_BASE_URL + CRYPTAUTH_METHOD_ENROLL_KEYS, authToken, jsonBody)
}

internal data class CryptAuthEnrollmentKey(
    val name: String,
    val handle: ByteArray,
    val material: ByteArray,
    val proof: ByteArray,
)

private suspend fun cryptAuthQuery(url: String, authToken: String, requestBody: JSONObject) = withContext(
    Dispatchers.IO) {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        setRequestMethod("POST")
        setDoInput(true)
        setDoOutput(true)
        connectTimeout = CRYPTAUTH_NETWORK_TIMEOUT_MILLIS
        readTimeout = CRYPTAUTH_NETWORK_TIMEOUT_MILLIS
        setRequestProperty("x-goog-api-key", API_KEY)
        setRequestProperty("x-android-package", Constants.GMS_PACKAGE_NAME)
        setRequestProperty("x-android-cert", CERTIFICATE)
        setRequestProperty("Authorization", "Bearer $authToken")
        setRequestProperty("Content-Type", "application/json")
    }

    try {
        connection.outputStream.use { output ->
            output.write(requestBody.toString().toByteArray())
        }
        if (connection.responseCode != 200) {
            var error = connection.getResponseMessage()
            try {
                connection.errorStream?.use { error = String(Utils.readStreamToEnd(it)) }
            } catch (e: IOException) {
                // Ignore
            }
            throw IOException(error)
        }

        val result = connection.inputStream.use { String(Utils.readStreamToEnd(it)) }
        try {
            JSONObject(result)
        } catch (e: Exception) {
            null
        }
    } finally {
        connection.disconnect()
    }
}

fun <K, V> jsonObjectOf(vararg pairs: Pair<K, V>): JSONObject = JSONObject(mapOf(*pairs))

internal fun Context.isLockscreenConfigured(): Boolean {
    val service = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    return if (android.os.Build.VERSION.SDK_INT >= 23) {
        service.isDeviceSecure
    } else {
        service.isKeyguardSecure
    }
}
