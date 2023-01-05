/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.appcert

import android.content.Context
import android.database.Cursor
import android.os.SystemClock
import android.util.Base64
import android.util.Log
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.Volley
import com.google.android.gms.BuildConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.ByteString.Companion.of
import org.microg.gms.checkin.LastCheckinInfo
import org.microg.gms.common.Constants
import org.microg.gms.common.PackageUtils
import org.microg.gms.droidguard.core.DroidGuardResultCreator
import org.microg.gms.gcm.GcmConstants
import org.microg.gms.gcm.GcmDatabase
import org.microg.gms.gcm.RegisterRequest
import org.microg.gms.gcm.completeRegisterRequest
import org.microg.gms.profile.Build
import org.microg.gms.profile.ProfileManager
import org.microg.gms.settings.SettingsContract.CheckIn
import org.microg.gms.settings.SettingsContract.getSettings
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

class AppCertManager(private val context: Context) {
    private val queue = Volley.newRequestQueue(context)

    private fun readDeviceKey() {
        try {
            if (context.getFileStreamPath("device_key").exists()) {
                deviceKey = context.openFileInput("device_key").use { DeviceKey.ADAPTER.decode(it) }
                deviceKeyCacheTime = context.getFileStreamPath("device_key").lastModified()
            } else {
                deviceKeyCacheTime = -1
            }
        } catch (e: Exception) {
            deviceKeyCacheTime = -1
        }
    }

    suspend fun fetchDeviceKey(): Boolean {
        ProfileManager.ensureInitialized(context)
        if (deviceKeyCacheTime == 0L) readDeviceKey()
        deviceKeyLock.withLock {
            try {
                val currentTime = System.currentTimeMillis()
                if (deviceKeyCacheTime > 0 && currentTime - deviceKeyCacheTime < DEVICE_KEY_TIMEOUT) {
                    return deviceKey != null
                }
                Log.w(TAG, "DeviceKeys for app certifications are experimental")
                deviceKeyCacheTime = currentTime
                val lastCheckinInfo = LastCheckinInfo.read(context)
                val androidId = lastCheckinInfo.androidId
                val sessionId = Random.nextLong()
                val data = hashMapOf(
                        "dg_androidId" to androidId.toString(16),
                        "dg_session" to sessionId.toString(16),
                        "dg_gmsCoreVersion" to BuildConfig.VERSION_CODE.toString(),
                        "dg_sdkVersion" to Build.VERSION.SDK_INT.toString()
                )
                val droidGuardResult = try {
                    DroidGuardResultCreator.getResults(context, "devicekey", data)
                } catch (e: Exception) {
                    null
                }
                val token = completeRegisterRequest(context, GcmDatabase(context), RegisterRequest().build(context)
                        .checkin(lastCheckinInfo)
                        .app("com.google.android.gms", Constants.GMS_PACKAGE_SIGNATURE_SHA1, BuildConfig.VERSION_CODE)
                        .sender(REGISTER_SENDER)
                        .extraParam("subscription", REGISTER_SUBSCIPTION)
                        .extraParam("X-subscription", REGISTER_SUBSCIPTION)
                        .extraParam("subtype", REGISTER_SUBTYPE)
                        .extraParam("X-subtype", REGISTER_SUBTYPE)
                        .extraParam("scope", REGISTER_SCOPE))
                        .getString(GcmConstants.EXTRA_REGISTRATION_ID)
                val request = DeviceKeyRequest(
                        droidGuardResult = droidGuardResult,
                        androidId = lastCheckinInfo.androidId,
                        sessionId = sessionId,
                        versionInfo = DeviceKeyRequest.VersionInfo(Build.VERSION.SDK_INT, BuildConfig.VERSION_CODE),
                        token = token
                )
                Log.d(TAG, "Request: ${request.toString().chunked(128).joinToString("\n")}")
                val deferredResponse = CompletableDeferred<ByteArray?>()
                queue.add(object : Request<ByteArray?>(Method.POST, "https://android.googleapis.com/auth/devicekey", null) {
                    override fun getBody(): ByteArray = request.encode()

                    override fun getBodyContentType(): String = "application/octet-stream"

                    override fun parseNetworkResponse(response: NetworkResponse): Response<ByteArray?> {
                        return if (response.statusCode == 200) {
                            Response.success(response.data, null)
                        } else {
                            Response.success(null, null)
                        }
                    }

                    override fun deliverError(error: VolleyError) {
                        Log.d(TAG, "Error: ${Base64.encodeToString(error.networkResponse.data, 2)}")
                        deviceKeyCacheTime = 0
                        deferredResponse.complete(null)
                    }

                    override fun deliverResponse(response: ByteArray?) {
                        deferredResponse.complete(response)
                    }

                    override fun getHeaders(): Map<String, String> {
                        return mapOf(
                                "User-Agent" to "GoogleAuth/1.4 (${Build.DEVICE} ${Build.ID}); gzip",
                                "content-type" to "application/octet-stream",
                                "app" to "com.google.android.gms",
                                "device" to androidId.toString(16)
                        )
                    }
                })
                val deviceKeyBytes = deferredResponse.await() ?: return false
                context.openFileOutput("device_key", Context.MODE_PRIVATE).use {
                    it.write(deviceKeyBytes)
                }
                context.getFileStreamPath("device_key").setLastModified(currentTime)
                deviceKey = DeviceKey.ADAPTER.decode(deviceKeyBytes)
                return true
            } catch (e: Exception) {
                Log.w(TAG, e)
                return false
            }
        }
    }

    suspend fun getSpatulaHeader(packageName: String): String? {
        val deviceKey = deviceKey ?: if (fetchDeviceKey()) deviceKey else null
        val packageCertificateHash = Base64.encodeToString(PackageUtils.firstSignatureDigestBytes(context, packageName), Base64.NO_WRAP)
        val proto = if (deviceKey != null) {
            val macSecret = deviceKey.macSecret?.toByteArray()
            if (macSecret == null) {
                Log.w(TAG, "Invalid device key: $deviceKey")
                return null
            }
            val mac = Mac.getInstance("HMACSHA256")
            mac.init(SecretKeySpec(macSecret, "HMACSHA256"))
            val hmac = mac.doFinal("$packageName$packageCertificateHash".toByteArray())
            SpatulaHeaderProto(
                    packageInfo = SpatulaHeaderProto.PackageInfo(packageName, packageCertificateHash),
                    hmac = of(*hmac),
                    deviceId = deviceKey.deviceId,
                    keyId = deviceKey.keyId,
                    keyCert = deviceKey.keyCert ?: of()
            )
        } else {
            Log.d(TAG, "Using fallback spatula header based on Android ID")
            val androidId = getSettings(context, CheckIn.getContentUri(context), arrayOf(CheckIn.ANDROID_ID, CheckIn.SECURITY_TOKEN)) { cursor: Cursor -> cursor.getLong(0) }
            SpatulaHeaderProto(
                    packageInfo = SpatulaHeaderProto.PackageInfo(packageName, packageCertificateHash),
                    deviceId = androidId
            )
        }
        Log.d(TAG, "Spatula Header: $proto")
        return Base64.encodeToString(proto.encode(), Base64.NO_WRAP)
    }

    companion object {
        private const val TAG = "AppCertManager"
        private const val DEVICE_KEY_TIMEOUT = 60 * 60 * 1000L
        private const val REGISTER_SENDER = "745476177629"
        private const val REGISTER_SUBTYPE = "745476177629"
        private const val REGISTER_SUBSCIPTION = "745476177629"
        private const val REGISTER_SCOPE = "DeviceKeyRequest"
        private val deviceKeyLock = Mutex()
        private var deviceKey: DeviceKey? = null
        private var deviceKeyCacheTime = 0L
    }
}
