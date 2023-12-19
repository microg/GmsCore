package com.google.android.gms.potokens.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import com.android.volley.AuthFailureError
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
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.microg.gms.common.Constants
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Random
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs

class PoTokenHelper {

    private val tokenUrl: String
        get() = "https://deviceintegritytokens-pa.googleapis.com/v1/getPoIntegrityToken?alt=proto&key=AIzaSyBtL0AK6Hzgr69rQyeyhi-V1lmtsPGZd1M"

    private fun buildKeySet(): KeySet {
        val builder: KeySet.Builder = KeySet.Builder()
        val keyBuilder: Key.Builder = Key.Builder()
        val initKey = ByteArray(16)
        SecureRandom().nextBytes(initKey)
        val keyId = abs(Random().nextInt())
        val byteString = initKey.toByteString()
        val cipherKey: CipherKey = CipherKey.Builder().value_(byteString).build()
        val keyData: KeyData = KeyData.Builder()
            .typeUrl("type.googleapis.com/google.crypto.tink.AesGcmKey")
            .keyMaterialType(1)
            .value_(cipherKey).build()
        keyBuilder.data_(keyData)
        keyBuilder.keyId(keyId)
        keyBuilder.status(1)
        keyBuilder.outputPrefixType(1)
        builder.keyList(arrayListOf(keyBuilder.build()))
        builder.primaryKeyId(keyId)
        return builder.build()
    }

    private fun concatCipherIdentifier(keyId: Int, data: ByteArray): ByteArray {
        val output = ByteArray(5 + data.size)
        output[0] = 1
        val identifier = ByteBuffer.allocate(5).put(1.toByte()).putInt(keyId).array()
        System.arraycopy(identifier, 0, output, 0, identifier.size)
        System.arraycopy(data, 0, output, identifier.size, data.size)
        return output
    }

    private fun aesEncrypt(key: ByteArray?, iv: ByteArray?, data: ByteArray): ByteArray {
        try {
            val output = ByteArray(data.size + 28)
            iv?.let { System.arraycopy(it, 0, output, 0, 12) }
            val keySpec = SecretKeySpec(key, "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(iv))
            cipher.doFinal(data, 0, data.size, output, 12)
            return output
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return ByteArray(0)
    }

    private fun getDroidGuardResult(
        context: Context?,
        request: DroidGuardResultsRequest?,
        map: Map<String, String>?
    ): String? {
        try {
            val resultTask = DroidGuard.getClient(context).getResults("po-token-fast", map, request)
            return Tasks.await(resultTask, 15, TimeUnit.SECONDS)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return null
    }

    private fun getPoIntegrityToken(ctx: Context?, keySet: KeySet?): GetPoIntegrityTokenResponse? {
        try {
            val request = DroidGuardResultsRequest()
            request.setTimeoutMillis(180000)
            request.setOpenHandles(2)
            request.setClientVersion(220221045)
            keySet?.let {
                request.bundle.putByteArray("po-fast-key", it.encode())
            }
            val tmpList = ArrayList<String>()
            tmpList.add("po-fast-key")
            request.bundle.putStringArrayList("extraKeysRetainedInFallback", tmpList)

            val secureRandom = SecureRandom()
            val randKeyBuf = ByteArray(0x20)
            secureRandom.nextBytes(randKeyBuf)
            val randKey = Base64.encodeToString(randKeyBuf, Base64.NO_WRAP)
            val map: MutableMap<String, String> = HashMap()
            map["b"] = randKey
            val getPoIntegrityTokenRequestBuilder = GetPoIntegrityTokenRequest.Builder()
            getDroidGuardResult(ctx, request, map)?.let {
                getPoIntegrityTokenRequestBuilder.dgResult = it.encodeToByteArray().toByteString()
            }
            getPoIntegrityTokenRequestBuilder.dgRandKey = randKeyBuf.toByteString()
            getPoIntegrityTokenRequestBuilder.mode = 1
            val getPoIntegrityTokenRequest = getPoIntegrityTokenRequestBuilder.build()

            return postPoTokenForGms(ctx, getPoIntegrityTokenRequest)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return null
    }

    private fun postPoTokenForGms(
        context: Context?,
        request: GetPoIntegrityTokenRequest,
    ): GetPoIntegrityTokenResponse? {
        val future = RequestFuture.newFuture<GetPoIntegrityTokenResponse>()
        Log.d(TAG,"postPoTokenForGms ")
        Volley.newRequestQueue(context).add(object : Request<GetPoIntegrityTokenResponse>(
            Method.POST, tokenUrl, future) {

            override fun deliverResponse(response: GetPoIntegrityTokenResponse?) {
                future.onResponse(response)
            }

            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                return mapOf("User-Agent" to  "GmsCore/${Constants.GMS_VERSION_CODE} (${Build.DEVICE} ${Build.ID}); gzip")
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

    fun callPoToken(ctx: Context, packageName: String?, inputData: ByteArray?): ByteArray {
        var keySet: KeySet? = null
        val spUtil = SpUtil[ctx]
        var tokenDesc = spUtil.getString("tokenDesc", "")
        var tokenBackup = spUtil.getString("tokenBackup", "")
        var keySetStr = spUtil.getString("keySetStr", "")

        if (TextUtils.isEmpty(tokenDesc) || TextUtils.isEmpty(tokenBackup) || TextUtils.isEmpty(keySetStr)) {
            keySet = buildKeySet()
            val response = PoTokenHelper().getPoIntegrityToken(ctx, keySet)
            tokenDesc = Base64.encodeToString(response?.desc?.toByteArray(), Base64.DEFAULT)
            tokenBackup = Base64.encodeToString(response?.backup?.toByteArray(), Base64.DEFAULT)
            keySetStr = Base64.encodeToString(keySet.encode(), Base64.DEFAULT)
            spUtil.save("tokenDesc", tokenDesc)
            spUtil.save("tokenBackup", tokenBackup)
            spUtil.save("keySetStr", keySetStr)
        } else if (!TextUtils.isEmpty(keySetStr)) {
            val result = Base64.decode(keySetStr, Base64.DEFAULT)
            keySet = KeySet.ADAPTER.decode(result)
        }

        val poTokenInfoBuilder: PoTokenInfo.Builder = PoTokenInfo.Builder()
        inputData?.let { poTokenInfoBuilder.inputData(it.toByteString()) }
        packageName?.let { poTokenInfoBuilder.pkgName(it) }
        getPackageSignatures(ctx, packageName)?.let { poTokenInfoBuilder.pkgSignSha256(it.toByteString()) }
        val tokenDescByteString: ByteString = Base64.decode(tokenDesc, Base64.DEFAULT).toByteString()
        val tokenBackupByteString: ByteString = Base64.decode(tokenBackup, Base64.DEFAULT).toByteString()
        poTokenInfoBuilder.piTokenData(tokenDescByteString)
        var data: ByteArray = poTokenInfoBuilder.build().encode()
        val keyObj: Key? = keySet?.keyList?.get(0)
        val keyId: Int? = keyObj?.keyId
        val key: ByteArray? = keyObj?.data_?.value_?.value_?.toByteArray()
        val iv = ByteArray(12)
        val secureRandom = SecureRandom()
        secureRandom.nextBytes(iv)
        data = aesEncrypt(key, iv, data)
        keyId?.let { data = concatCipherIdentifier(it, data) }
        val resultBuilder: PoTokenResult.Builder = PoTokenResult.Builder()
        resultBuilder.encryptData(data.toByteString())
        resultBuilder.piTokenData(tokenBackupByteString)
        return PoTokenResultWrap.Builder().data_(resultBuilder.build()).build().encode()
    }

    private fun getPackageSignatures(context: Context, packageName: String?): ByteArray? {
        try {
            val info = context.packageManager.getPackageInfo(packageName!!, PackageManager.GET_SIGNATURES)
            if (info?.signatures != null && info.signatures.isNotEmpty()) {
                for (signature in info.signatures) {
                    val md = MessageDigest.getInstance("SHA-256")
                    md.update(signature.toByteArray())
                    return md.digest()
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return null
    }
}
