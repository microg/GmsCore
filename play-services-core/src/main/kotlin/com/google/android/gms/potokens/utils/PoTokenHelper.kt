/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.potokens.utils

import android.content.Context
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
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
import com.google.android.gms.potokens.internal.TAG
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.toByteString
import org.microg.gms.common.Constants
import org.microg.gms.profile.Build
import org.microg.gms.utils.getFirstSignatureDigest
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.Random
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs

class PoTokenHelper(context: Context) {

    private val volleyQueue = Volley.newRequestQueue(context)

    private fun buildKeySet(): KeySet {
        val keyId = abs(Random().nextInt())
        val byteString = ByteArray(16).also { SecureRandom().nextBytes(it) }.toByteString()
        return KeySet(
            keyId = keyId,
            keyList = listOf(
                Key(
                    data_ = KeyData(
                        typeUrl = PoTokenConstants.TYPE_URL,
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
            val resultTask = DroidGuard.getClient(context).getResults(PoTokenConstants.KEY_TOKEN, map, request)
            return Tasks.await(resultTask, 15, TimeUnit.SECONDS)
        } catch (e: Throwable) {
            Log.w(TAG, "PoTokenHelper getDroidGuardResult exception: $e")
        }
        return null
    }

    private fun getPoIntegrityToken(context: Context, keySet: KeySet): GetPoIntegrityTokenResponse? {
        try {
            val droidGuardResultsRequest = DroidGuardResultsRequest().apply {
                bundle.putByteArray(PoTokenConstants.KEY_FAST, keySet.encode())
                bundle.putStringArrayList(PoTokenConstants.KEY_FALLBACK, arrayListOf(PoTokenConstants.KEY_FAST))
            }
            val randKeyBuf = ByteArray(0x20).also { SecureRandom().nextBytes(it) }
            val randKey = Base64.encodeToString(randKeyBuf, Base64.NO_WRAP)
            val map: MutableMap<String, String> = HashMap()
            map["b"] = randKey
            val dgResult =
                getDroidGuardResult(context, droidGuardResultsRequest, map)?.encodeToByteArray()?.toByteString()
            val tokenRequest =
                GetPoIntegrityTokenRequest(dgResult = dgResult, dgRandKey = randKeyBuf.toByteString(), mode = 1)
            return postPoTokenForGms(tokenRequest)
        } catch (e: Throwable) {
            Log.w(TAG, "PoTokenHelper getPoIntegrityToken exception: $e")
        }
        return null
    }

    private fun postPoTokenForGms(request: GetPoIntegrityTokenRequest): GetPoIntegrityTokenResponse? {
        val future = RequestFuture.newFuture<GetPoIntegrityTokenResponse>()
        volleyQueue.add(object : Request<GetPoIntegrityTokenResponse>(
            Method.POST, PoTokenConstants.TOKEN_URL, future
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
        return future.get()
    }

    suspend fun callPoToken(context: Context, packageName: String, inputData: ByteArray): ByteArray {
        var tokenDesc = PoTokenPreferences.get(context).getString(PoTokenConstants.KEY_DESC, "")
        var tokenBackup = PoTokenPreferences.get(context).getString(PoTokenConstants.KEY_BACKUP, "")
        var keySetStr = PoTokenPreferences.get(context).getString(PoTokenConstants.KEY_SET_STR, "")

        val keySet =
            if (TextUtils.isEmpty(tokenDesc) || TextUtils.isEmpty(tokenBackup) || TextUtils.isEmpty(keySetStr)) {
                buildKeySet().also {
                    Log.d(TAG, "PoTokenHelper postPoTokenForGms start")
                    val response = withContext(Dispatchers.IO) { getPoIntegrityToken(context, it) }
                    Log.d(TAG, "PoTokenHelper postPoTokenForGms end")
                    tokenDesc = Base64.encodeToString(response?.desc?.toByteArray(), Base64.DEFAULT)
                    tokenBackup = Base64.encodeToString(response?.backup?.toByteArray(), Base64.DEFAULT)
                    keySetStr = Base64.encodeToString(it.encode(), Base64.DEFAULT)
                    PoTokenPreferences.get(context).save(PoTokenConstants.KEY_DESC, tokenDesc)
                    PoTokenPreferences.get(context).save(PoTokenConstants.KEY_BACKUP, tokenBackup)
                    PoTokenPreferences.get(context).save(PoTokenConstants.KEY_SET_STR, keySetStr)
                }
            } else {
                val result = Base64.decode(keySetStr, Base64.DEFAULT)
                KeySet.ADAPTER.decode(result)
            }

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
        keyId?.let { data = concatCipherIdentifier(it, data) }

        val poTokenResult = PoTokenResult(
            encryptData = data.toByteString(),
            tokenData = Base64.decode(tokenBackup, Base64.DEFAULT).toByteString()
        )

        return PoTokenResultWrap(poTokenResult).encode()
    }

    companion object {
        @Volatile
        private var instance: PoTokenHelper? = null
        fun get(context: Context): PoTokenHelper {
            return instance ?: synchronized(this) {
                instance ?: PoTokenHelper(context).also { instance = it }
            }
        }
    }

}
