/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.appcert

import android.content.Context
import android.database.Cursor
import android.util.Base64
import android.util.Log
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.Volley
import com.google.android.gms.BuildConfig
import com.google.android.gms.droidguard.DroidGuardClient
import com.google.android.gms.tasks.await
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.ByteString.Companion.of
import org.microg.gms.checkin.LastCheckinInfo
import org.microg.gms.profile.Build
import org.microg.gms.profile.ProfileManager
import org.microg.gms.settings.SettingsContract.CheckIn
import org.microg.gms.settings.SettingsContract.getSettings
import org.microg.gms.utils.digest
import org.microg.gms.utils.getCertificates
import org.microg.gms.utils.singleInstanceOf
import org.microg.gms.utils.toBase64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

class AppCertManager(private val context: Context) {
    private val queue = singleInstanceOf { Volley.newRequestQueue(context.applicationContext) }

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
                        "dg_androidId" to java.lang.Long.toHexString(androidId),
                        "dg_session" to java.lang.Long.toHexString(sessionId),
                        "dg_gmsCoreVersion" to BuildConfig.VERSION_CODE.toString(),
                        "dg_sdkVersion" to Build.VERSION.SDK_INT.toString()
                )
                val droidGuardResult = try {
                    DroidGuardClient.getResults(context, "devicekey", data).await()
                } catch (e: Exception) {
                    Log.w(TAG, "DG devicekey failed: ${e.message}")
                    null
                }
                Log.i(TAG, "DG devicekey result: ${if (droidGuardResult != null) "${droidGuardResult.length} chars" else "null"}, androidId=${java.lang.Long.toHexString(androidId)}")
                val token = DEVICE_KEY_TOKEN_PLACEHOLDER
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

                    override fun getBodyContentType(): String = "application/x-protobuf"

                    override fun parseNetworkResponse(response: NetworkResponse): Response<ByteArray?> {
                        Log.i(TAG, "devicekey HTTP ${response.statusCode}, ${response.data?.size ?: 0} bytes")
                        return if (response.statusCode == 200) {
                            Response.success(response.data, null)
                        } else {
                            Log.w(TAG, "devicekey HTTP ${response.statusCode} body: ${String(response.data ?: ByteArray(0)).take(200)}")
                            Response.success(null, null)
                        }
                    }

                    override fun deliverError(error: VolleyError) {
                        if (error.networkResponse != null) {
                            Log.d(TAG, "Error: ${Base64.encodeToString(error.networkResponse.data, 2)}")
                        } else {
                            Log.d(TAG, "Error: ${error.message}")
                        }
                        deviceKeyCacheTime = 0
                        deferredResponse.complete(null)
                    }

                    override fun deliverResponse(response: ByteArray?) {
                        deferredResponse.complete(response)
                    }

                    override fun getHeaders(): Map<String, String> {
                        return mapOf(
                                "app" to java.util.UUID.randomUUID().toString(),
                                "device" to java.lang.Long.toHexString(androidId),
                                "gmsversion" to BuildConfig.VERSION_CODE.toString(),
                                "gmscoreFlow" to "3"
                        )
                    }
                })
                val deviceKeyBytes = deferredResponse.await()
                if (deviceKeyBytes == null) {
                    Log.w(TAG, "devicekey fetch returned null (HTTP error)")
                    return false
                }
                Log.i(TAG, "devicekey SUCCESS: ${deviceKeyBytes.size} bytes")
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
        // Try fetch/refresh; even if fetchDeviceKey() returns false (e.g. HTTP 400),
        // readDeviceKey() inside it may have loaded a valid key from disk.
        if (deviceKey == null) fetchDeviceKey()
        val deviceKey = deviceKey
        val packageCertificateHash = context.packageManager.getCertificates(packageName).firstOrNull()?.digest("SHA1")?.toBase64(Base64.NO_WRAP)
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
            val androidId = getSettings(context, CheckIn.getContentUri(context), arrayOf(CheckIn.ANDROID_ID)) { cursor: Cursor -> cursor.getLong(0) }
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
        private const val DEVICE_KEY_TOKEN_PLACEHOLDER = "missing_token"
        private val deviceKeyLock = Mutex()
        private var deviceKey: DeviceKey? = null
        private var deviceKeyCacheTime = 0L
    }
}
