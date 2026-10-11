package org.microg.gms.cryptauth

import android.content.Context
import com.google.android.gms.BuildConfig
import cryptauthv2.ApplicationSpecificMetadata
import cryptauthv2.ClientAppMetadata
import cryptauthv2.SyncKeysRequest
import cryptauthv2.SyncKeysResponse
import cryptauthv2.EnrollKeysRequest
import cryptauthv2.EnrollKeysResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.json.JSONArray
import org.json.JSONObject
import org.microg.gms.common.Constants
import org.microg.gms.common.DeviceConfiguration
import org.microg.gms.common.Utils
import org.microg.gms.profile.Build
import java.io.IOException
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

private const val CRYPTAUTH_BASE_URL = "https://cryptauthenrollment.googleapis.com/"
private const val CRYPTAUTH_METHOD_SYNC_KEYS = "v1:syncKeys"
private const val CRYPTAUTH_METHOD_ENROLL_KEYS = "v1:enrollKeys"

private const val API_KEY = "AIzaSyAP-gfH3qvi6vgHZbSYwQ_XHqV_mXHhzIk"
internal const val CERTIFICATE = "58E1C4133F7441EC3D2C270270A14802DA47BA0E"

internal const val CRYPTAUTH_FIELD_SESSION_ID = "randomSessionId"


internal fun Context.cryptAuthMetadata(instanceId: String, instanceToken: String, androidId: Long): ByteString {
    val deviceConfig = DeviceConfiguration(this)
    return ClientAppMetadata(
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
}

internal class CryptAuthHttpTransport(private val authToken: String) : CryptAuthTransport {
    override suspend fun sync(request: SyncKeysRequest): SyncKeysResponse = SyncKeysResponse.ADAPTER.decode(
        cryptAuthQuery(CRYPTAUTH_METHOD_SYNC_KEYS, authToken, request.encode(), "application/x-protobuf", true)
    )

    override suspend fun enroll(request: EnrollKeysRequest): EnrollKeysResponse = EnrollKeysResponse.ADAPTER.decode(
        cryptAuthQuery(CRYPTAUTH_METHOD_ENROLL_KEYS, authToken, request.encode(), "application/x-protobuf", true)
    )
}

// Retain the pre-existing metadata-only screen-lock flow on all supported Android versions.
// A successful result here does not imply enrollment or account-transfer support.
internal suspend fun Context.cryptAuthSyncKeys(authToken: String, instanceId: String, instanceToken: String, androidId: Long): JSONObject? {
    val clientAppMetadata = cryptAuthMetadata(instanceId, instanceToken, androidId).base64Url()

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

    return JSONObject(String(cryptAuthQuery(CRYPTAUTH_METHOD_SYNC_KEYS, authToken,
        jsonBody.toString().toByteArray(Charsets.UTF_8), "application/json"), Charsets.UTF_8))
}

internal suspend fun Context.cryptAuthEnrollKeys(authToken: String, session: String): JSONObject? {
    val jsonBody = jsonObjectOf(
        CRYPTAUTH_FIELD_SESSION_ID to session,
        "clientEphemeralDh" to "",
        "enrollSingleKeyRequests" to JSONArray(),
    )

    return JSONObject(String(cryptAuthQuery(CRYPTAUTH_METHOD_ENROLL_KEYS, authToken,
        jsonBody.toString().toByteArray(Charsets.UTF_8), "application/json"), Charsets.UTF_8))
}

private suspend fun cryptAuthQuery(method: String, authToken: String, body: ByteArray,
                                  contentType: String, protobuf: Boolean = false): ByteArray = withContext(
    Dispatchers.IO) {
    currentCoroutineContext().ensureActive()
    val connection = (URL(CRYPTAUTH_BASE_URL + method + if (protobuf) "?alt=proto" else "").openConnection() as HttpURLConnection).apply {
        setRequestMethod("POST")
        setDoInput(true)
        setDoOutput(true)
        setRequestProperty("x-goog-api-key", API_KEY)
        setRequestProperty("x-android-package", Constants.GMS_PACKAGE_NAME)
        setRequestProperty("x-android-cert", CERTIFICATE)
        setRequestProperty("Authorization", "Bearer $authToken")
        setRequestProperty("Content-Type", contentType)
        setRequestProperty("Accept", contentType)
        instanceFollowRedirects = false
        connectTimeout = 20_000
        readTimeout = 20_000
        setFixedLengthStreamingMode(body.size)
    }
    try {
        connection.outputStream.use { it.write(body) }
        val status = connection.responseCode
        // Do not expose response bodies, registration tokens or account details through exceptions/logs.
        if (status != HttpURLConnection.HTTP_OK) throw IOException("CryptAuth HTTP status $status")
        val bytes = connection.inputStream.use { input ->
            val result = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = input.read(buffer)
                if (count < 0) break
                if (result.size() + count > 1024 * 1024) throw IOException("CryptAuth response exceeds limit")
                result.write(buffer, 0, count)
            }
            result.toByteArray()
        }
        currentCoroutineContext().ensureActive()
        bytes
    } finally { connection.disconnect() }
}

fun <K, V> jsonObjectOf(vararg pairs: Pair<K, V>): JSONObject = JSONObject(mapOf(*pairs))
inline fun <reified T> jsonArrayOf(vararg elements: T): JSONArray = JSONArray(arrayOf(*elements))
