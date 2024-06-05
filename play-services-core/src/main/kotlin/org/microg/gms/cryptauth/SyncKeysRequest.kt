package org.microg.gms.cryptauth

import android.accounts.Account
import android.app.KeyguardManager
import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.android.gms.BuildConfig
import com.google.android.gms.common.Scopes
import cryptauthv2.ApplicationSpecificMetadata
import cryptauthv2.ClientAppMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.toByteString
import org.json.JSONArray
import org.json.JSONObject
import org.microg.gms.auth.AuthConstants
import org.microg.gms.auth.AuthManager
import org.microg.gms.checkin.LastCheckinInfo
import org.microg.gms.common.Constants
import org.microg.gms.common.DeviceConfiguration
import org.microg.gms.common.Utils
import org.microg.gms.gcm.GcmConstants
import org.microg.gms.gcm.GcmDatabase
import org.microg.gms.gcm.RegisterRequest
import org.microg.gms.gcm.completeRegisterRequest
import org.microg.gms.profile.Build
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyPairGenerator
import java.security.MessageDigest
import kotlin.math.roundToInt

private const val SYNC_KEY_URL = "https://cryptauthenrollment.googleapis.com/v1:syncKeys"
private const val API_KEY = "AIzaSyAP-gfH3qvi6vgHZbSYwQ_XHqV_mXHhzIk"
private const val CERTIFICATE = "58E1C4133F7441EC3D2C270270A14802DA47BA0E"
private const val TAG = "SyncKeysRequest"

private const val GCM_REGISTER_SENDER = "16502139086"
private const val GCM_REGISTER_SUBTYPE = "16502139086"
private const val GCM_REGISTER_SUBSCRIPTION = "16502139086"
private const val GCM_REGISTER_SCOPE = "GCM"
private const val GCM_REGISTER_CLIV = "iid-202414000"
private const val GCM_REGISTER_INFO = "0wqs6iYsl_URQEb1aBJ6XhzCbHSr-hg"

private const val RSA_KEY_SIZE = 2048

private const val AFTER_REQUEST_DELAY = 750L

suspend fun syncCryptAuthKeys(context: Context, account: Account): JSONObject? = syncCryptAuthKeys(context, account.name)
suspend fun syncCryptAuthKeys(context: Context, accountName: String): JSONObject? {

    val lastCheckinInfo = LastCheckinInfo.read(context)

    // Instance ID token for use in CryptAuth query
    val instanceId = generateAppId()
    val instanceToken = completeRegisterRequest(context, GcmDatabase(context), RegisterRequest().build(context)
        .checkin(lastCheckinInfo)
        .app("com.google.android.gms", CERTIFICATE.lowercase(), BuildConfig.VERSION_CODE)
        .sender(GCM_REGISTER_SENDER)
        .extraParam("subscription", GCM_REGISTER_SUBSCRIPTION)
        .extraParam("X-subscription", GCM_REGISTER_SUBSCRIPTION)
        .extraParam("subtype", GCM_REGISTER_SUBTYPE)
        .extraParam("X-subtype", GCM_REGISTER_SUBTYPE)
        .extraParam("app_ver", BuildConfig.VERSION_CODE.toString())
        .extraParam("osv", "29")
        .extraParam("cliv", GCM_REGISTER_CLIV)
        .extraParam("gmsv", BuildConfig.VERSION_CODE.toString())
        .extraParam("appid", instanceId)
        .extraParam("scope", GCM_REGISTER_SCOPE)
        .extraParam("app_ver_name","%09d".format(BuildConfig.VERSION_CODE).let {
            "${it.substring(0, 2)}.${it.substring(2, 4)}.${it.substring(4, 6)} (190800-{{cl}})"
        })
        .info(GCM_REGISTER_INFO)
    ).let {
        if (!it.containsKey(GcmConstants.EXTRA_REGISTRATION_ID)) {
            Log.d(TAG, "No instance ID was gathered. Is GCM enabled, has there been a checkin?")
            return null
        }
        it.getString(GcmConstants.EXTRA_REGISTRATION_ID)!!
    }

    // CryptAuth sync request tells server whether or not screenlock is enabled
    val cryptauthService = AuthConstants.SCOPE_OAUTH2 + Scopes.CRYPTAUTH
    val authManager = AuthManager(context, accountName, Constants.GMS_PACKAGE_NAME, cryptauthService)
    val authToken = withContext(Dispatchers.IO) { authManager.requestAuth(false).auth }

    val deviceConfig = DeviceConfiguration(context)
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
        android_device_id = lastCheckinInfo.androidId,
        locale = Utils.getLocale(context).toString().replace("_", "-"),
        device_os_version = Build.DISPLAY ?: "",
        device_os_version_code = Build.VERSION.SDK_INT.toLong(),
        device_os_release = Build.VERSION.CODENAME?: "",
        device_display_diagonal_mils = (deviceConfig.diagonalInch / 1000).roundToInt(),
        device_model = Build.MODEL ?: "",
        device_manufacturer = Build.MANUFACTURER ?: "",
        device_type = ClientAppMetadata.DeviceType.ANDROID,
        using_secure_screenlock = context.isLockscreenConfigured(),
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

    return withContext(Dispatchers.IO) {
        val connection = (URL(SYNC_KEY_URL).openConnection() as HttpURLConnection).apply {
            setRequestMethod("POST")
            setDoInput(true)
            setDoOutput(true)
            setRequestProperty("x-goog-api-key", API_KEY)
            setRequestProperty("x-android-package", Constants.GMS_PACKAGE_NAME)
            setRequestProperty("x-android-cert", CERTIFICATE)
            setRequestProperty("Authorization", "Bearer $authToken")
            setRequestProperty("Content-Type", "application/json")
        }
        Log.d(TAG, "-- Request --\n$jsonBody")

        val os = connection.outputStream
        os.write(jsonBody.toString().toByteArray())
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
                .also {
                    /* Give Google server some time to process the new information.
                     * This leads to higher success rate compared to sending
                     * the next query immediately after this one.
                     */
                    delay(AFTER_REQUEST_DELAY)
                }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Generates an app / instance ID based on the hash of the public key of an RSA keypair.
 * The key itself is never used.
 */
fun generateAppId(): String {
    val rsaGenerator = KeyPairGenerator.getInstance("RSA")
    rsaGenerator.initialize(RSA_KEY_SIZE)
    val keyPair = rsaGenerator.generateKeyPair()

    val digest = MessageDigest.getInstance("SHA1").digest(keyPair.public.encoded)
    digest[0] = ((112 + (0xF and digest[0].toInt())) and 0xFF).toByte()
    return Base64.encodeToString(
        digest, 0, 8, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
    )
}

fun Context.isLockscreenConfigured(): Boolean {
    val service: KeyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        service.isDeviceSecure
    } else {
        service.isKeyguardSecure
    }
}

fun <K, V> jsonObjectOf(vararg pairs: Pair<K, V>): JSONObject = JSONObject(mapOf(*pairs))
inline fun <reified T> jsonArrayOf(vararg elements: T): JSONArray = JSONArray(arrayOf(*elements))