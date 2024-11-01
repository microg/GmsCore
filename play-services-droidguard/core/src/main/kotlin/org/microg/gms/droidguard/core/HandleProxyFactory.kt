/*
 * SPDX-FileCopyrightText: 2021 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import android.content.Context
import androidx.annotation.GuardedBy
import com.android.volley.NetworkResponse
import com.android.volley.VolleyError
import com.android.volley.toolbox.RequestFuture
import com.android.volley.toolbox.Volley
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest
import dalvik.system.DexClassLoader
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.of
import org.microg.gms.droidguard.*
import org.microg.gms.profile.Build
import org.microg.gms.profile.ProfileManager
import org.microg.gms.utils.singleInstanceOf
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.security.cert.Certificate
import java.util.*
import com.android.volley.Request as VolleyRequest
import com.android.volley.Response as VolleyResponse

class HandleProxyFactory(private val context: Context) {
    @GuardedBy("CLASS_LOCK")
    private val classMap = hashMapOf<String, Class<*>>()
    private val dgDb: DgDatabaseHelper = DgDatabaseHelper(context)
    private val version = VersionUtil(context)
    private val queue = singleInstanceOf { Volley.newRequestQueue(context.applicationContext) }

    fun createHandle(packageName: String, flow: String?, callback: GuardCallback, request: DroidGuardResultsRequest?): HandleProxy {
        if (!DroidGuardPreferences.isLocalAvailable(context)) throw IllegalAccessException("DroidGuard should not be available locally")
        val (vmKey, byteCode, bytes) = readFromDatabase(flow) ?: fetchFromServer(flow, packageName)
        return createHandleProxy(flow, vmKey, byteCode, bytes, callback, request)
    }

    fun createPingHandle(packageName: String, flow: String, callback: GuardCallback, pingData: PingData?): HandleProxy {
        if (!DroidGuardPreferences.isLocalAvailable(context)) throw IllegalAccessException("DroidGuard should not be available locally")
        val (vmKey, byteCode, bytes) = fetchFromServer(flow, createRequest(flow, packageName, pingData))
        return createHandleProxy(flow, vmKey, byteCode, bytes, callback, DroidGuardResultsRequest().also { it.clientVersion = 0 })
    }

    fun createLowLatencyHandle(flow: String?, callback: GuardCallback, request: DroidGuardResultsRequest?): HandleProxy {
        if (!DroidGuardPreferences.isLocalAvailable(context)) throw IllegalAccessException("DroidGuard should not be available locally")
        val (vmKey, byteCode, bytes) = readFromDatabase("fast") ?: throw Exception("low latency (fast) flow not available")
        return createHandleProxy(flow, vmKey, byteCode, bytes, callback, request)
    }

    fun SignedResponse.unpack(): Response {
        if (SignatureVerifier.verifySignature(data_!!.toByteArray(), signature!!.toByteArray())) {
            return Response.ADAPTER.decode(data_!!)
        } else {
            throw SecurityException("Signature invalid")
        }
    }

    private fun readFromDatabase(flow: String?): Triple<String, ByteArray, ByteArray>? {
        ProfileManager.ensureInitialized(context)
        val id = "$flow/${version.versionString}/${Build.FINGERPRINT}"
        return dgDb.get(id)
    }

    fun createRequest(flow: String?, packageName: String, pingData: PingData? = null, extra: ByteArray? = null): Request {
        ProfileManager.ensureInitialized(context)
        return Request(
                usage = Usage(flow, packageName),
                info = listOf(
                        KeyValuePair("BOARD", Build.BOARD),
                        KeyValuePair("BOOTLOADER", Build.BOOTLOADER),
                        KeyValuePair("BRAND", Build.BRAND),
                        KeyValuePair("CPU_ABI", Build.CPU_ABI),
                        KeyValuePair("CPU_ABI2", Build.CPU_ABI2),
                        KeyValuePair("SUPPORTED_ABIS", Build.SUPPORTED_ABIS.joinToString(",")),
                        KeyValuePair("DEVICE", Build.DEVICE),
                        KeyValuePair("DISPLAY", Build.DISPLAY),
                        KeyValuePair("FINGERPRINT", Build.FINGERPRINT),
                        KeyValuePair("HARDWARE", Build.HARDWARE),
                        KeyValuePair("HOST", Build.HOST),
                        KeyValuePair("ID", Build.ID),
                        KeyValuePair("MANUFACTURER", Build.MANUFACTURER),
                        KeyValuePair("MODEL", Build.MODEL),
                        KeyValuePair("PRODUCT", Build.PRODUCT),
                        KeyValuePair("RADIO", Build.RADIO),
                        KeyValuePair("SERIAL", Build.SERIAL),
                        KeyValuePair("TAGS", Build.TAGS),
                        KeyValuePair("TIME", Build.TIME.toString()),
                        KeyValuePair("TYPE", Build.TYPE),
                        KeyValuePair("USER", Build.USER),
                        KeyValuePair("VERSION.CODENAME", Build.VERSION.CODENAME),
                        KeyValuePair("VERSION.INCREMENTAL", Build.VERSION.INCREMENTAL),
                        KeyValuePair("VERSION.RELEASE", Build.VERSION.RELEASE),
                        KeyValuePair("VERSION.SDK", Build.VERSION.SDK),
                        KeyValuePair("VERSION.SDK_INT", Build.VERSION.SDK_INT.toString()),
                ),
                versionName = version.versionString,
                versionCode = BuildConfig.VERSION_CODE,
                hasAccount = false,
                isGoogleCn = false,
                enableInlineVm = true,
                cached = getCacheDir().list()?.map { it.decodeHex() }.orEmpty(),
                arch = System.getProperty("os.arch"),
                ping = pingData,
                field10 = extra?.let { of(*it) },
        )
    }

    fun fetchFromServer(flow: String?, packageName: String): Triple<String, ByteArray, ByteArray> {
        return fetchFromServer(flow, createRequest(flow, packageName))
    }

    fun fetchFromServer(flow: String?, request: Request): Triple<String, ByteArray, ByteArray> {
        ProfileManager.ensureInitialized(context)
        val future = RequestFuture.newFuture<SignedResponse>()
        queue.add(object : VolleyRequest<SignedResponse>(Method.POST, SERVER_URL, future) {
            override fun parseNetworkResponse(response: NetworkResponse): VolleyResponse<SignedResponse> {
                return try {
                    VolleyResponse.success(SignedResponse.ADAPTER.decode(response.data), null)
                } catch (e: Exception) {
                    VolleyResponse.error(VolleyError(e))
                }
            }

            override fun deliverResponse(response: SignedResponse) {
                future.onResponse(response)
            }

            override fun getBody(): ByteArray = request.encode()

            override fun getBodyContentType(): String = "application/x-protobuf"

            override fun getHeaders(): Map<String, String> {
                return mapOf(
                        "User-Agent" to "DroidGuard/${version.versionCode}"
                )
            }
        })
        val signed: SignedResponse = future.get()
        val response = signed.unpack()
        val vmKey = response.vmChecksum!!.hex()
        if (!isValidCache(vmKey)) {
            val temp = File(getCacheDir(), "${UUID.randomUUID()}.apk")
            temp.parentFile!!.mkdirs()
            temp.writeBytes(response.content!!.toByteArray())
            getOptDir(vmKey).mkdirs()
            temp.renameTo(getTheApkFile(vmKey))
            updateCacheTimestamp(vmKey)
            if (!isValidCache(vmKey)) {
                getCacheDir(vmKey).deleteRecursively()
                throw IllegalStateException()
            }
        }
        val id = "$flow/${version.versionString}/${Build.FINGERPRINT}"
        val expiry = (response.expiryTimeSecs ?: 0).toLong()
        val byteCode = response.byteCode?.toByteArray() ?: ByteArray(0)
        val extra = response.extra?.toByteArray() ?: ByteArray(0)
        if (response.save != false) {
            dgDb.put(id, expiry, vmKey, byteCode, extra)
        }
        return Triple(vmKey, byteCode, extra)
    }

    private fun createHandleProxy(
        flow: String?,
        vmKey: String,
        byteCode: ByteArray,
        extra: ByteArray,
        callback: GuardCallback,
        request: DroidGuardResultsRequest?
    ): HandleProxy {
        ProfileManager.ensureInitialized(context)
        val clazz = loadClass(vmKey, extra)
        return HandleProxy(clazz, context, flow, byteCode, callback, vmKey, extra, request?.bundle)
    }

    fun getTheApkFile(vmKey: String) = File(getCacheDir(vmKey), "the.apk")
    private fun getCacheDir() = context.getDir(CACHE_FOLDER_NAME, Context.MODE_PRIVATE)
    private fun getCacheDir(vmKey: String) = File(getCacheDir(), vmKey)
    private fun getOptDir(vmKey: String) = File(getCacheDir(vmKey), "opt")
    private fun isValidCache(vmKey: String) = getTheApkFile(vmKey).isFile && getOptDir(vmKey).isDirectory

    private fun updateCacheTimestamp(vmKey: String) {
        try {
            val timestampFile = File(getCacheDir(vmKey), "t")
            if (!timestampFile.exists() && !timestampFile.createNewFile()) {
                throw Exception("Failed to touch last-used file for $vmKey.")
            }
            if (!timestampFile.setLastModified(System.currentTimeMillis())) {
                throw Exception("Failed to update last-used timestamp for $vmKey.")
            }
        } catch (e: IOException) {
            throw Exception("Failed to touch last-used file for $vmKey.")
        }
    }

    private fun verifyApkSignature(apk: File): Boolean {
        return true
        val certificates: Array<Certificate> = TODO()
        if (certificates.size != 1) return false
        return Arrays.equals(MessageDigest.getInstance("SHA-256").digest(certificates[0].encoded), PROD_CERT_HASH)
    }

    private fun loadClass(vmKey: String, bytes: ByteArray): Class<*> {
        synchronized(CLASS_LOCK) {
            val cachedClass = classMap[vmKey]
            if (cachedClass != null) {
                updateCacheTimestamp(vmKey)
                return cachedClass
            }
            val weakClass = weakClassMap[vmKey]
            if (weakClass != null) {
                classMap[vmKey] = weakClass
                updateCacheTimestamp(vmKey)
                return weakClass
            }
            if (!isValidCache(vmKey)) {
                throw BytesException(bytes, "VM key $vmKey not found in cache")
            }
            if (!verifyApkSignature(getTheApkFile(vmKey))) {
                getCacheDir(vmKey).deleteRecursively()
                throw ClassNotFoundException("APK signature verification failed")
            }
            val loader = DexClassLoader(getTheApkFile(vmKey).absolutePath, getOptDir(vmKey).absolutePath, null, context.classLoader)
            val clazz = loader.loadClass(CLASS_NAME)
            classMap[vmKey] = clazz
            weakClassMap[vmKey] = clazz
            return clazz
        }
    }

    companion object {
        const val CLASS_NAME = "com.google.ccc.abuse.droidguard.DroidGuard"
        const val SERVER_URL = "https://www.googleapis.com/androidantiabuse/v1/x/create?alt=PROTO&key=AIzaSyBofcZsgLSS7BOnBjZPEkk4rYwzOIz-lTI"
        const val CACHE_FOLDER_NAME = "cache_dg"
        val CLASS_LOCK = Object()
        @GuardedBy("CLASS_LOCK")
        val weakClassMap = WeakHashMap<String, Class<*>>()
        val PROD_CERT_HASH = byteArrayOf(61, 122, 18, 35, 1, -102, -93, -99, -98, -96, -29, 67, 106, -73, -64, -119, 107, -5, 79, -74, 121, -12, -34, 95, -25, -62, 63, 50, 108, -113, -103, 74)
    }
}
