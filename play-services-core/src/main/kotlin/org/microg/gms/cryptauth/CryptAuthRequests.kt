package org.microg.gms.cryptauth

import android.content.Context
import android.util.Log
import com.google.android.gms.BuildConfig
import cryptauthv2.ApplicationSpecificMetadata
import cryptauthv2.ClientAppMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.toByteString
import org.json.JSONArray
import org.json.JSONObject
import org.microg.gms.common.Constants
import org.microg.gms.common.DeviceConfiguration
import org.microg.gms.common.Utils
import org.microg.gms.profile.Build
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

private const val TAG = "CryptAuthRequests"

private const val CRYPTAUTH_BASE_URL = "https://cryptauthenrollment.googleapis.com/"
private const val CRYPTAUTH_METHOD_SYNC_KEYS = "v1:syncKeys"
private const val CRYPTAUTH_METHOD_ENROLL_KEYS = "v1:enrollKeys"

private const val API_KEY = "AIzaSyAP-gfH3qvi6vgHZbSYwQ_XHqV_mXHhzIk"
internal const val CERTIFICATE = "58E1C4133F7441EC3D2C270270A14802DA47BA0E"

internal const val CRYPTAUTH_FIELD_SESSION_ID = "randomSessionId"


internal suspend fun Context.cryptAuthSyncKeys(authToken: String, instanceId: String, instanceToken: String, androidId: Long): JSONObject? {
    // CryptAuth sync request tells server whether or not screenlock is enabled

    val deviceConfig = DeviceConfiguration(this)
    val clientAppMetadata = ClientAppMetadata(
        application_specific_metadata = listOf(
            ApplicationSpecificMetadata(
                gcm_registration_id = instanceToken.toByteArray().toByteString(),
                notification_enabled = true,
                device_software_version = "%09d".format(BuildConfig.VERSION_CODE).let {
                    "${it.substring(0, 2)}.${it.substring(2, 4)}.${it.substring(4, 6)} (190800-{{cl}})"
                },
                device_software_version_code = BuildConfig.VERSION_CODE.toLong(),
                device_software_package = Constants.GMS_PACKAGE_NAME
            )
        ),
        instance_id = instanceId,
        instance_id_token = instanceToken,
        android_device_id = androidId,
        locale = Utils.getLocale(this).toString().replace("_", "-"),
        device_os_version = Build.DISPLAY ?: "",
        device_os_version_code = Build.VERSION.SDK_INT.toLong(),
        device_os_release = Build.VERSION.CODENAME?: "",
        device_display_diagonal_mils = (deviceConfig.diagonalInch / 1000).roundToInt(),
        device_model = Build.MODEL ?: "",
        device_manufacturer = Build.MANUFACTURER ?: "",
        device_type = ClientAppMetadata.DeviceType.ANDROID,
        using_secure_screenlock = isLockscreenConfigured(),
        bluetooth_radio_supported = true, // TODO actual value? doesn't seem relevant
        // bluetooth_radio_enabled = false,
        ble_radio_supported = true, // TODO: actual value? doesn't seem relevant
        mobile_data_supported = true, // TODO: actual value? doesn't seem relevant
        // droid_guard_response = "…"
    )
        .encodeByteString()
        .base64Url()

    val jsonBody = jsonObjectOf(
        "applicationName" to Constants.GMS_PACKAGE_NAME,
        "clientVersion" to "1.0.0",
        "syncSingleKeyRequests" to jsonArrayOf(
            jsonObjectOf(
                "keyName" to "PublicKey",
                "keyHandles" to "ZGV2aWNlX2tleQo=" // base64 for `device_key`
            )
        ),
        "clientMetadata" to jsonObjectOf(
            "invocationReason" to "NEW_ACCOUNT"
        ),
        "clientAppMetadata" to clientAppMetadata,
    )

    return cryptAuthQuery(CRYPTAUTH_BASE_URL + CRYPTAUTH_METHOD_SYNC_KEYS, authToken, jsonBody)
}

internal suspend fun Context.cryptAuthEnrollKeys(authToken: String, session: String): JSONObject? {
    val jsonBody = jsonObjectOf(
        CRYPTAUTH_FIELD_SESSION_ID to session,
        "clientEphemeralDh" to "",
        "enrollSingleKeyRequests" to JSONArray(),
    )

    return cryptAuthQuery(CRYPTAUTH_BASE_URL + CRYPTAUTH_METHOD_ENROLL_KEYS, authToken, jsonBody)
}

private suspend fun Context.cryptAuthQuery(url: String, authToken: String, requestBody: JSONObject) = withContext(
    Dispatchers.IO) {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        setRequestMethod("POST")
        setDoInput(true)
        setDoOutput(true)
        setRequestProperty("x-goog-api-key", API_KEY)
        setRequestProperty("x-android-package", Constants.GMS_PACKAGE_NAME)
        setRequestProperty("x-android-cert", CERTIFICATE)
        setRequestProperty("Authorization", "Bearer $authToken")
        setRequestProperty("Content-Type", "application/json")
    }

    Log.d(TAG, "-- Request --\n$requestBody")
    val os = connection.outputStream
    os.write(requestBody.toString().toByteArray())
    os.close()

    if (connection.getResponseCode() != 200) {
        var error = connection.getResponseMessage()
        try {
            error = String(Utils.readStreamToEnd(connection.errorStream))
        } catch (e: IOException) {
            // Ignore
        }
        throw IOException(error)
    }

    val result = String(Utils.readStreamToEnd(connection.inputStream))
    Log.d(TAG, "-- Response --\n$result")
    try {
        JSONObject(result)

    } catch (e: Exception) {
        null
    }
}

fun <K, V> jsonObjectOf(vararg pairs: Pair<K, V>): JSONObject = JSONObject(mapOf(*pairs))
inline fun <reified T> jsonArrayOf(vararg elements: T): JSONArray = JSONArray(arrayOf(*elements))