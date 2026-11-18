/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.potokens

import android.content.Context
import android.os.SystemClock
import android.util.Base64
import android.util.Log
import com.android.volley.NetworkError
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.TimeoutError
import com.android.volley.VolleyError
import com.android.volley.toolbox.RequestFuture
import com.android.volley.toolbox.Volley
import com.google.android.gms.droidguard.DroidGuard
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest
import com.google.android.gms.potokens.CipherKey
import com.google.android.gms.potokens.GetPoIntegrityTokenRequest
import com.google.android.gms.potokens.GetPoIntegrityTokenResponse
import com.google.android.gms.potokens.Key
import com.google.android.gms.potokens.KeyData
import com.google.android.gms.potokens.KeySet
import com.google.android.gms.potokens.PoTokenInfo
import com.google.android.gms.potokens.PoTokenResult
import com.google.android.gms.potokens.PoTokenResultWrap
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.toByteString
import org.microg.gms.common.Constants
import org.microg.gms.profile.Build
import org.microg.gms.utils.getFirstSignatureDigest
import org.microg.gms.utils.singleInstanceOf
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.Random
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs

private const val PO_TOKEN_REQUEST_TIMEOUT_SECONDS = 60L

private class InvalidDroidGuardResultException(size: Int?) : Exception(
    "DroidGuard result is null or shorter than 100 bytes (size=$size)"
)

class PoTokenHelper(val context: Context) {
    private var accessTokenLimitCount = 0
    private var accessTokenLimitIntervalCount = 0
    private var lastIntervalTriggerTime: Long? = null
    private val poTokenMutex = Mutex()
    private val volleyQueue = singleInstanceOf { Volley.newRequestQueue(context.applicationContext) }
    private val poTokenStore = singleInstanceOf { PoTokenStore(context.applicationContext) }

    private fun buildKeySet(): KeySet {
        val keyId = abs(Random().nextInt())
        val byteString = ByteArray(16).also { SecureRandom().nextBytes(it) }.toByteString()
        return KeySet(
            keyId = keyId,
            keyList = listOf(
                Key(
                    data_ = KeyData(
                        typeUrl = TYPE_URL,
                        keyMaterialType = 1,
                        value_ = CipherKey(value_ = byteString)
                    ),
                    keyId = keyId,
                    status = 1,
                    outputPrefixType = 1,
                )
            )
        )
    }

    private fun concatCipherIdentifier(keyId: Int, data: ByteArray): ByteArray {
        val output = ByteArray(5 + data.size)
        output[0] = 1
        val identifier = ByteBuffer.allocate(5).put(1.toByte()).putInt(keyId).array()
        System.arraycopy(identifier, 0, output, 0, identifier.size)
        System.arraycopy(data, 0, output, identifier.size, data.size)
        return output
    }

    private fun aesEncrypt(key: ByteArray?, iv: ByteArray, data: ByteArray): ByteArray {
        try {
            val output = ByteArray(data.size + 28)
            System.arraycopy(iv, 0, output, 0, 12)
            val keySpec = SecretKeySpec(key, "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(iv))
            cipher.doFinal(data, 0, data.size, output, 12)
            return output
        } catch (e: Throwable) {
            Log.w(TAG, "PoTokenHelper aesEncrypt exception: $e")
        }
        return ByteArray(0)
    }

    private fun getDroidGuardResult(
        context: Context,
        request: DroidGuardResultsRequest,
        map: Map<String, String>
    ): String? {
        try {
            val resultTask = DroidGuard.getClient(context).getResults(KEY_TOKEN, map, request)
            return Tasks.await(resultTask, 15, TimeUnit.SECONDS)
        } catch (e: Throwable) {
            Log.w(TAG, "PoTokenHelper getDroidGuardResult exception: $e")
        }
        return null
    }

    private fun getPoIntegrityToken(keySet: KeySet): GetPoIntegrityTokenResponse? {
        try {
            val droidGuardResultsRequest = DroidGuardResultsRequest().apply {
                bundle.putByteArray(KEY_FAST, keySet.encode())
                bundle.putStringArrayList(KEY_FALLBACK, arrayListOf(KEY_FAST))
            }
            val randKeyBuf = ByteArray(0x20).also { SecureRandom().nextBytes(it) }
            val randKey = Base64.encodeToString(randKeyBuf, Base64.NO_WRAP)
            val map: MutableMap<String, String> = HashMap()
            map["b"] = randKey
            val dgResult =
                getDroidGuardResult(context, droidGuardResultsRequest, map)?.encodeToByteArray()?.toByteString()
            // Normal DroidGuard results are expected to exceed 100 bytes.
            // Treat results smaller than 100 bytes as potentially invalid.
            if (dgResult == null || dgResult.size < 100) {
                throw InvalidDroidGuardResultException(dgResult?.size)
            }
            val tokenRequest =
                GetPoIntegrityTokenRequest(dgResult = dgResult, dgRandKey = randKeyBuf.toByteString(), mode = 1)
            return postPoTokenForGms(tokenRequest)
        } catch (e: Throwable) {
            Log.w(TAG, "PoTokenHelper getPoIntegrityToken exception: $e")
            throw e
        }
    }

    private fun postPoTokenForGms(request: GetPoIntegrityTokenRequest): GetPoIntegrityTokenResponse? {
        val future = RequestFuture.newFuture<GetPoIntegrityTokenResponse>()
        val networkRequest = volleyQueue.add(object : Request<GetPoIntegrityTokenResponse>(
            Method.POST, PO_INTEGRITY_TOKEN_SERVER_URL, future
        ) {

            override fun deliverResponse(response: GetPoIntegrityTokenResponse?) {
                Log.d(TAG, "PoTokenHelper postPoTokenForGms response: $response")
                future.onResponse(response)
            }

            override fun getHeaders(): Map<String, String> {
                return mapOf("User-Agent" to "GmsCore/${Constants.GMS_VERSION_CODE} (${Build.DEVICE} ${Build.ID}); gzip")
            }

            override fun getBody(): ByteArray {
                return request.encode()
            }

            override fun getBodyContentType(): String = "application/x-protobuf"

            override fun parseNetworkResponse(response: NetworkResponse): Response<GetPoIntegrityTokenResponse> {
                return try {
                    Response.success(GetPoIntegrityTokenResponse.ADAPTER.decode(response.data), null)
                } catch (e: Exception) {
                    Response.error(VolleyError(e))
                }
            }
        })
        return try {
            future.get(PO_TOKEN_REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            networkRequest.cancel()
            throw e
        }
    }

    private fun isNetworkOrTimeoutFailure(error: Throwable): Boolean {
        var cause = error
        while (cause is ExecutionException) {
            cause = cause.cause ?: return false
        }
        // HTTP and decoding errors must still consume the normal access quota.
        return cause is TimeoutException || cause is TimeoutError ||
                (cause is NetworkError && cause.networkResponse == null)
    }

    suspend fun callPoToken(packageName: String, inputData: ByteArray): ByteArray = poTokenMutex.withLock {
        var tokenInfo = poTokenStore.loadUsedIntegrityTokenInfo()
        val lastUpdateTime = poTokenStore.getLastUpdateTime()
        val flag = poTokenStore.getIntervalFlag()
        val nowElapsed = SystemClock.elapsedRealtime()
        var nextAccessTokenLimitCount = accessTokenLimitCount
        var nextAccessTokenLimitIntervalCount = accessTokenLimitIntervalCount
        var nextLastIntervalTriggerTime = lastIntervalTriggerTime
        var nextIntervalFlag: Boolean? = null
        var updateAccessTime = false
        Log.d(TAG, "callPoToken start lastUpdateTime: $lastUpdateTime")
        if (!flag && lastIntervalTriggerTime != null &&
            nowElapsed - lastIntervalTriggerTime!! >= PO_TOKEN_ACCESS_INTERVAL_COUNT_RESET_TIME
        ) {
            Log.w(TAG,"callPoToken: resetting interval count after inactivity " +
                        "(count=$nextAccessTokenLimitIntervalCount)")
            nextAccessTokenLimitIntervalCount = 0
            nextLastIntervalTriggerTime = null
        }
        if (flag || nextAccessTokenLimitIntervalCount > PO_TOKEN_ACCESS_LIMIT_COUNT) {
            nextIntervalFlag = true
            val intervalTime = System.currentTimeMillis() - lastUpdateTime
            Log.d(TAG, "callPoToken run Interval: $intervalTime")
            if (intervalTime > PO_TOKEN_ACCESS_INTERVAL_TIME) {
                nextIntervalFlag = false
                nextAccessTokenLimitIntervalCount = 0
                nextLastIntervalTriggerTime = null
                tokenInfo = null
                Log.d(TAG, "callPoToken run Interval over")
            }
        } else {
            Log.d(TAG, "callPoToken run Limit: $accessTokenLimitCount")
            if (System.currentTimeMillis() - lastUpdateTime < PO_TOKEN_ACCESS_LIMIT_TIME) {
                if (accessTokenLimitCount < PO_TOKEN_ACCESS_LIMIT_COUNT) {
                    nextAccessTokenLimitCount++
                } else {
                    nextAccessTokenLimitIntervalCount++
                    nextLastIntervalTriggerTime = nowElapsed
                    nextAccessTokenLimitCount = 0
                    tokenInfo = null
                }
            } else {
                nextAccessTokenLimitCount = 0
            }
            updateAccessTime = true
        }

        val (poTokenResultBytes, recordAccess) = poTokenStore.buildPoToken(tokenInfo, packageName, inputData)
        if (recordAccess) {
            accessTokenLimitCount = nextAccessTokenLimitCount
            accessTokenLimitIntervalCount = nextAccessTokenLimitIntervalCount
            lastIntervalTriggerTime = nextLastIntervalTriggerTime
            nextIntervalFlag?.let { poTokenStore.updateIntervalFlag(it) }
            if (updateAccessTime) {
                poTokenStore.updateLastUpdateTime()
            }
        }
        poTokenResultBytes
    }

    private suspend fun refreshIntegrityToken(store: PoTokenStore): IntegrityTokenInfo = refresh {
        val keySet = buildKeySet()
        Log.d(TAG, "buildPoToken postPoTokenForGms start")
        val response = withContext(Dispatchers.IO) { getPoIntegrityToken(keySet) }
        val desc = response?.desc
        val backup = response?.backup
        if (desc == null || desc.size == 0 || backup == null || backup.size == 0) {
            throw RuntimeException("buildPoToken -> response token data is missing or empty")
        }
        val tokenInfo = IntegrityTokenInfo(
            Base64.encodeToString(keySet.encode(), Base64.DEFAULT),
            Base64.encodeToString(desc.toByteArray(), Base64.DEFAULT),
            Base64.encodeToString(backup.toByteArray(), Base64.DEFAULT),
            System.currentTimeMillis()
        )
        store.saveUsedTokenInfo(tokenInfo)
        store.clearOldTokenInfo()
        Log.d(TAG, "buildPoToken postPoTokenForGms end")
        tokenInfo
    }

    suspend fun PoTokenStore.buildPoToken(tokenInfo: IntegrityTokenInfo?,packageName: String, inputData: ByteArray): Pair<ByteArray, Boolean> = try {
        val currentTokenInfo = if (tokenInfo == null || tokenInfo.token.isNullOrEmpty() ||
            tokenInfo.tokenBackUp.isNullOrEmpty() || tokenInfo.key.isNullOrEmpty()) {
            refreshIntegrityToken(this)
        } else {
            Log.d(TAG, "PoTokenHelper buildPoToken used old keySet")
            tokenInfo
        }

        buildPoTokenFromInfo(currentTokenInfo, packageName, inputData) to true
    } catch (e: Exception) {
        val recordAccess = e !is CancellationException && e !is InvalidDroidGuardResultException &&
                !isNetworkOrTimeoutFailure(e)
        Log.w(TAG, "buildPoToken: error: ", e)
        val cachedResult = try {
            val cachedTokenInfo = loadUsedIntegrityTokenInfo() ?: tokenInfo
            cachedTokenInfo?.let { buildPoTokenFromInfo(it, packageName, inputData) }
        } catch (cacheError: Exception) {
            Log.w(TAG, "buildPoToken: cached integrity token could not be used", cacheError)
            null
        }
        if (cachedResult != null) {
            Log.d(TAG, "buildPoToken: using cached integrity token after failure")
        }
        (cachedResult ?: PoTokenResultWrap().encode()) to recordAccess
    }

    private fun buildPoTokenFromInfo(tokenInfo: IntegrityTokenInfo, packageName: String, inputData: ByteArray): ByteArray {
        val tokenDesc = tokenInfo.token
        val tokenBackup = tokenInfo.tokenBackUp
        val keySetStr = tokenInfo.key
        require(!tokenDesc.isNullOrEmpty() && !tokenBackup.isNullOrEmpty() && !keySetStr.isNullOrEmpty()) {
            "buildPoToken -> token data is missing or empty"
        }
        val keySet = KeySet.ADAPTER.decode(Base64.decode(keySetStr, Base64.DEFAULT))
        Log.d(TAG, "buildPoToken: keySetStr: $keySetStr tokenDesc: $tokenDesc tokenBackup: $tokenBackup")

        val poTokenInfoData: ByteArray = PoTokenInfo(
            inputData = inputData.toByteString(),
            pkgName = packageName,
            pkgSignSha256 = context.packageManager.getFirstSignatureDigest(packageName, "SHA-256")?.toByteString(),
            tokenData = Base64.decode(tokenDesc, Base64.DEFAULT).toByteString()
        ).encode()

        val keyObj: Key = keySet.keyList[0]
        val keyId: Int? = keyObj.keyId
        val key: ByteArray? = keyObj.data_?.value_?.value_?.toByteArray()
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }

        var data = aesEncrypt(key, iv, poTokenInfoData)
        check(data.isNotEmpty()) { "buildPoToken -> encryption failed" }
        keyId?.let { data = concatCipherIdentifier(it, data) }

        val poTokenResult = PoTokenResult(
            encryptData = data.toByteString(),
            tokenData = Base64.decode(tokenBackup, Base64.DEFAULT).toByteString()
        )

        return PoTokenResultWrap(poTokenResult).encode()
    }

    companion object {
        private val mutex = Mutex()
        // Cancelling one caller must not cancel the refresh awaited by other callers.
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private var inFlight: Deferred<IntegrityTokenInfo>? = null

        suspend fun refresh(fetch: suspend () -> IntegrityTokenInfo): IntegrityTokenInfo {
            val request = mutex.withLock {
                inFlight?.takeUnless { it.isCompleted }
                    ?: scope.async { fetch() }.also { inFlight = it }
            }
            return request.await()
        }
    }
}
