/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky

import android.accounts.AccountManager
import android.accounts.AccountManagerFuture
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Binder
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import com.android.vending.buildRequestHeaders
import com.android.vending.makeTimestamp
import com.google.android.finsky.expressintegrityservice.ExpressIntegritySession
import com.google.android.finsky.expressintegrityservice.IntermediateIntegrityResponseData
import com.google.android.gms.droidguard.DroidGuard
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest
import com.google.android.gms.tasks.await
import com.google.crypto.tink.Aead
import com.google.crypto.tink.BinaryKeysetReader
import com.google.crypto.tink.BinaryKeysetWriter
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AesGcmKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.ByteString
import okio.ByteString.Companion.encode
import okio.ByteString.Companion.toByteString
import org.microg.gms.profile.Build
import org.microg.vending.billing.DEFAULT_ACCOUNT_TYPE
import org.microg.vending.billing.GServices
import org.microg.vending.billing.core.HttpClient
import org.microg.vending.proto.Timestamp
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.ProviderException
import java.security.spec.ECGenParameterSpec
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

const val INTEGRITY_PREFIX_ERROR = "ERROR :"
const val INTEGRITY_FLOW_NAME = "pia_attest_e1"
const val EXPRESS_INTEGRITY_FLOW_NAME = "pia_express"

const val KEY_PACKAGE_NAME = "package.name"
const val KEY_NONCE = "nonce"
const val KEY_CLOUD_PROJECT = "cloud.prj"
const val KEY_REQUEST_MODE = "webview.request.mode"
const val KEY_OPT_PACKAGE = "opted.out.pkg"
const val KEY_DROID_GUARD_SESSION_TOKEN_V1 = "droid_guard_session_token_v1"
const val KEY_REQUEST_VERDICT_OPT_OUT = "request.verdict.opt.out"
const val KEY_REQUEST_TOKEN_SID = "request.token.sid"
const val KEY_WARM_UP_SID = "warm.up.sid"
const val KEY_ERROR = "error"
const val KEY_TOKEN = "token"

const val PARAMS_PKG_KEY = "pkg_key"
const val PARAMS_VC_KEY = "vc_key"
const val PARAMS_NONCE_SHA256_KEY = "nonce_sha256_key"
const val PARAMS_TM_S_KEY = "tm_s_key"
const val PARAMS_BINDING_KEY = "binding_key"
const val PARAMS_GCP_N_KEY = "gcp_n_key"
const val PARAMS_PIA_EXPRESS_DEVICE_KEY = "piaExpressDeviceKey"

const val RESULT_UN_AUTH = "<UNAUTH>"

private const val KEY_VERSION_MAJOR = "playcore.integrity.version.major"
private const val KEY_VERSION_MINOR = "playcore.integrity.version.minor"
private const val KEY_VERSION_PATCH = "playcore.integrity.version.patch"

private const val DEVICE_INTEGRITY_SOFT_EXPIRATION_CHECK_PERIOD = 600L // 10 minutes
private const val TEMPORARY_DEVICE_KEY_VALIDITY = 64800L // 18 hours
private const val DEVICE_INTEGRITY_SOFT_EXPIRATION = 100800L // 28 hours
private const val DEVICE_INTEGRITY_HARD_EXPIRATION = 432000L // 5 day

private const val TAG = "IntegrityExtensions"

private fun Context.getProtoFile(): File {
    val directory = File(filesDir, "finsky/shared")
    if (!directory.exists()) {
        directory.mkdirs()
    }
    val file = File(directory, "express_integrity_valuestore.pb")
    if (!file.exists()) {
        file.createNewFile()
    }
    return file
}

fun PackageManager.getPackageInfoCompat(packageName: String, flags: Int = 0): PackageInfo {
    return runCatching {
        if (Build.VERSION.SDK_INT >= 33) {
            getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            getPackageInfo(packageName, flags)
        }
    }.getOrDefault(getPackageInfo(packageName, flags))
}

val SIGNING_FLAGS = if (Build.VERSION.SDK_INT >= 28) {
    PackageManager.GET_SIGNING_CERTIFICATES
} else {
    @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES
}

val PackageInfo.signaturesCompat: Array<Signature>
    get() {
        return if (Build.VERSION.SDK_INT >= 28) {
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        } else {
            @Suppress("DEPRECATION") signatures
        }
    }

fun ByteArray.encodeBase64(noPadding: Boolean, noWrap: Boolean = true, urlSafe: Boolean = true): String {
    var flags = 0
    if (noPadding) flags = flags or Base64.NO_PADDING
    if (noWrap) flags = flags or Base64.NO_WRAP
    if (urlSafe) flags = flags or Base64.URL_SAFE
    return Base64.encodeToString(this, flags)
}

fun ByteArray.sha256(): ByteArray {
    return MessageDigest.getInstance("SHA-256").digest(this)
}

fun Bundle.buildPlayCoreVersion() = PlayCoreVersion(
    major = getInt(KEY_VERSION_MAJOR, 0), minor = getInt(KEY_VERSION_MINOR, 0), patch = getInt(KEY_VERSION_PATCH, 0)
)

fun readAes128GcmBuilderFromClientKey(clientKey: ClientKey?): Aead? {
    if (clientKey == null) {
        return null
    }
    return try {
        val keySetHandle = CleartextKeysetHandle.read(BinaryKeysetReader.withBytes(clientKey.keySetHandle?.toByteArray()))
        keySetHandle.getPrimitive(Aead::class.java)
    } catch (e: Exception) {
        null
    }
}

suspend fun getIntegrityRequestWrapper(context: Context, expressIntegritySession: ExpressIntegritySession, accountName: String) = withContext(Dispatchers.IO){
    fun getUpdatedWebViewRequestMode(webViewRequestMode: Int): Int {
        return when (webViewRequestMode) {
            in 0..2 -> webViewRequestMode + 1
            else -> 1
        }
    }
    val expressFilePB = FileInputStream(context.getProtoFile()).use { input -> ExpressFilePB.ADAPTER.decode(input) }
    expressFilePB.integrityRequestWrapper.filter { item ->
        TextUtils.equals(item.packageName, expressIntegritySession.packageName) && item.cloudProjectNumber == expressIntegritySession.cloudProjectVersion && getUpdatedWebViewRequestMode(
            expressIntegritySession.webViewRequestMode
        ) == getUpdatedWebViewRequestMode(
            item.webViewRequestMode ?: 0
        )
    }.firstOrNull { item ->
        TextUtils.equals(item.accountName, accountName)
    }
}

fun fetchCertificateChain(context: Context, attestationChallenge: ByteArray?): List<ByteString> {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        val devicePropertiesAttestationIncluded = context.packageManager.hasSystemFeature("android.software.device_id_attestation")
        val keyGenParameterSpecBuilder =
            KeyGenParameterSpec.Builder("integrity.api.key.alias", KeyProperties.PURPOSE_SIGN).apply {
                this.setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                this.setDigests(KeyProperties.DIGEST_SHA512)
                if (devicePropertiesAttestationIncluded) {
                    this.setAttestationChallenge(attestationChallenge)
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    this.setDevicePropertiesAttestationIncluded(devicePropertiesAttestationIncluded)
                }
            }
        val generator = KeyPairGenerator.getInstance("EC", "AndroidKeyStore")
        var generateKeyPair = false
        var keyPair: KeyPair? = null
        val exceptionClassesCaught = HashSet<Class<Exception>>()
        while (!generateKeyPair) {
            try {
                generator.initialize(keyGenParameterSpecBuilder.build())
                keyPair = generator.generateKeyPair()
                generateKeyPair = true
            } catch (e: Exception) {
                // Catch each exception class at most once.
                // If we've caught the exception before, tried to correct it, and still catch the
                // same exception, then we can't fix it and the exception should be thrown further
                if (exceptionClassesCaught.contains(e.javaClass)) {
                    break
                }
                exceptionClassesCaught.add(e.javaClass)
                if (e is ProviderException) {
                    keyGenParameterSpecBuilder.setAttestationChallenge(null)
                }
            }
        }
        if (keyPair == null) {
            Log.w(TAG, "Failed to create the key pair.")
            return emptyList()
        }
        val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val certificateChainList = keyStore.getCertificateChain("integrity.api.key.alias")?.let { chain ->
            chain.map { it.encoded.toByteString() }
        }
        if (certificateChainList.isNullOrEmpty()) {
            Log.w(TAG, "Failed to get the certificate chain.")
            return emptyList()
        }
        return certificateChainList
    } else {
        return emptyList()
    }
}

suspend fun updateLocalExpressFilePB(context: Context, intermediateIntegrityResponseData: IntermediateIntegrityResponseData) = withContext(Dispatchers.IO) {
    Log.d(TAG, "Writing AAR to express cache")
    val intermediateIntegrity = intermediateIntegrityResponseData.intermediateIntegrity
    val expressFilePB = FileInputStream(context.getProtoFile()).use { input -> ExpressFilePB.ADAPTER.decode(input) }

    val integrityResponseWrapper = IntegrityRequestWrapper.Builder().apply {
        accountName = intermediateIntegrity.accountName
        packageName = intermediateIntegrity.packageName
        cloudProjectNumber = intermediateIntegrity.cloudProjectNumber
        callerKey = intermediateIntegrity.callerKey
        webViewRequestMode = intermediateIntegrity.webViewRequestMode.let {
            when (it) {
                in 0..2 -> it + 1
                else -> 1
            }
        } - 1
        deviceIntegrityWrapper = DeviceIntegrityWrapper.Builder().apply {
            creationTime = intermediateIntegrity.callerKey.generated
            serverGenerated = intermediateIntegrity.serverGenerated
            deviceIntegrityToken = intermediateIntegrity.intermediateToken
        }.build()
    }.build()

    val requestList = expressFilePB.integrityRequestWrapper.toMutableList()

    for ((index, item) in requestList.withIndex()) {
        if (TextUtils.equals(item.packageName, intermediateIntegrity.packageName) && item.cloudProjectNumber == intermediateIntegrity.cloudProjectNumber && TextUtils.equals(
                item.accountName, intermediateIntegrity.accountName
            )
        ) {
            if (integrityResponseWrapper.webViewRequestMode == item.webViewRequestMode) {
                requestList[index] = integrityResponseWrapper
                val newExpressFilePB = expressFilePB.newBuilder().integrityRequestWrapper(requestList).build()
                FileOutputStream(context.getProtoFile()).use { output -> ExpressFilePB.ADAPTER.encode(output, newExpressFilePB) }
                return@withContext
            }
        }
    }
    requestList.add(integrityResponseWrapper)
    val newExpressFilePB = expressFilePB.newBuilder().integrityRequestWrapper(requestList).build()
    FileOutputStream(context.getProtoFile()).use { output -> ExpressFilePB.ADAPTER.encode(output, newExpressFilePB) }
}

suspend fun updateExpressSessionTime(context: Context, expressIntegritySession: ExpressIntegritySession, refreshWarmUpMethodTime: Boolean, refreshRequestMethodTime: Boolean) =
    withContext(Dispatchers.IO) {
        val packageName = if (expressIntegritySession.webViewRequestMode != 0) {
            "WebView_" + expressIntegritySession.packageName
        } else {
            expressIntegritySession.packageName
        }

        val expressFilePB = FileInputStream(context.getProtoFile()).use { input -> ExpressFilePB.ADAPTER.decode(input) }

        val clientKey = expressFilePB.integrityTokenTimeMap ?: IntegrityTokenTimeMap()
        val timeMutableMap = clientKey.newBuilder().timeMap.toMutableMap()

        if (refreshWarmUpMethodTime) {
            timeMutableMap[packageName] = IntegrityTokenTime.Builder().warmUpTokenTime(
                TokenTime.Builder().type(1).timestamp(makeTimestamp(System.currentTimeMillis())).build()
            ).build()
        }

        if (refreshRequestMethodTime) {
            timeMutableMap[packageName] = IntegrityTokenTime.Builder().requestTokenTime(
                TokenTime.Builder().type(1).timestamp(makeTimestamp(System.currentTimeMillis())).build()
            ).build()
        }

        val newExpressFilePB = expressFilePB.newBuilder().integrityTokenTimeMap(IntegrityTokenTimeMap.Builder().timeMap(timeMutableMap).build()).build()
        FileOutputStream(context.getProtoFile()).use { output -> ExpressFilePB.ADAPTER.encode(output, newExpressFilePB) }
    }

suspend fun updateExpressClientKey(context: Context) = withContext(Dispatchers.IO) {
    val expressFilePB = FileInputStream(context.getProtoFile()).use { input -> ExpressFilePB.ADAPTER.decode(input) }

    val oldClientKey = expressFilePB.clientKey ?: ClientKey()
    var clientKey = ClientKey.Builder().apply {
        val currentTimeMillis = System.currentTimeMillis()
        generated = Timestamp.Builder().seconds(currentTimeMillis / 1000).nanos((Math.floorMod(currentTimeMillis, 1000L) * 1000000).toInt()).build()
        val keySetHandle = KeysetHandle.generateNew(AesGcmKeyManager.aes128GcmTemplate())
        val outputStream = ByteArrayOutputStream()
        CleartextKeysetHandle.write(keySetHandle, BinaryKeysetWriter.withOutputStream(outputStream))
        this.keySetHandle = ByteBuffer.wrap(outputStream.toByteArray()).toByteString()
    }.build()
    if (oldClientKey.keySetHandle?.size != 0) {
        if (oldClientKey.generated?.seconds != null && clientKey.generated?.seconds != null && oldClientKey.generated.seconds < clientKey.generated?.seconds!!.minus(TEMPORARY_DEVICE_KEY_VALIDITY)) {
            clientKey = oldClientKey
        }
    }

    val newExpressFilePB = expressFilePB.newBuilder().clientKey(clientKey).build()
    FileOutputStream(context.getProtoFile()).use { output -> ExpressFilePB.ADAPTER.encode(output, newExpressFilePB) }
    clientKey
}

suspend fun updateExpressAuthTokenWrapper(context: Context, expressIntegritySession: ExpressIntegritySession, authToken: String, clientKey: ClientKey) = withContext(Dispatchers.IO) {
    var expressFilePB = FileInputStream(context.getProtoFile()).use { input -> ExpressFilePB.ADAPTER.decode(input) }

    val createTimeSeconds = expressFilePB.tokenWrapper?.deviceIntegrityWrapper?.creationTime?.seconds ?: 0
    val lastManualSoftRefreshTime = expressFilePB.tokenWrapper?.lastManualSoftRefreshTime?.seconds ?: 0
    if (createTimeSeconds < System.currentTimeMillis() - DEVICE_INTEGRITY_HARD_EXPIRATION) {
        expressFilePB = expressFilePB.newBuilder().tokenWrapper(regenerateToken(context, authToken, expressIntegritySession.packageName, clientKey)).build()
    } else if (lastManualSoftRefreshTime <= System.currentTimeMillis() - DEVICE_INTEGRITY_SOFT_EXPIRATION_CHECK_PERIOD && createTimeSeconds < System.currentTimeMillis() - DEVICE_INTEGRITY_SOFT_EXPIRATION) {
        expressFilePB = expressFilePB.newBuilder().tokenWrapper(regenerateToken(context, authToken, expressIntegritySession.packageName, clientKey)).build()
    }

    FileOutputStream(context.getProtoFile()).use { output -> ExpressFilePB.ADAPTER.encode(output, expressFilePB) }

    expressFilePB
}

private suspend fun regenerateToken(
    context: Context, authToken: String, packageName: String, clientKey: ClientKey
): AuthTokenWrapper {
    try {
        Log.d(TAG, "regenerateToken authToken:$authToken, packageName:$packageName, clientKey:$clientKey")
        val droidGuardSessionTokenResponse = requestDroidGuardSessionToken(context, authToken)

        if (droidGuardSessionTokenResponse.tokenWrapper == null) {
            throw RuntimeException("regenerateToken droidGuardSessionTokenResponse.tokenWrapper is Empty!")
        }

        val droidGuardTokenType = droidGuardSessionTokenResponse.tokenWrapper.tokenContent?.tokenType?.firstOrNull { it.type?.toInt() == 5 }
            ?: throw RuntimeException("regenerateToken droidGuardTokenType is null!")

        val droidGuardTokenSession = droidGuardTokenType.tokenSessionWrapper?.wrapper?.sessionContent?.session?.id
        if (droidGuardTokenSession.isNullOrEmpty()) {
            throw RuntimeException("regenerateToken droidGuardTokenSession is null")
        }

        val data = mutableMapOf(KEY_DROID_GUARD_SESSION_TOKEN_V1 to droidGuardTokenSession)
        val droidGuardData = withContext(Dispatchers.IO) {
            val droidGuardResultsRequest = DroidGuardResultsRequest().apply {
                bundle.putByteArray(PARAMS_PIA_EXPRESS_DEVICE_KEY, clientKey.keySetHandle?.toByteArray())
            }
            Log.d(TAG, "Running DroidGuard (flow: $EXPRESS_INTEGRITY_FLOW_NAME, data: $data)")
            DroidGuard.getClient(context).getResults(EXPRESS_INTEGRITY_FLOW_NAME, data, droidGuardResultsRequest).await().encode()
        }

        val deviceIntegrityTokenResponse = requestDeviceIntegrityToken(context, authToken, droidGuardTokenSession, droidGuardData)

        val deviceIntegrityTokenType = deviceIntegrityTokenResponse.tokenWrapper?.tokenContent?.tokenType?.firstOrNull { it.type?.toInt() == 5 }
            ?: throw RuntimeException("regenerateToken deviceIntegrityTokenType is null!")

        val deviceIntegrityToken = deviceIntegrityTokenType.tokenSessionWrapper?.wrapper?.sessionContent?.tokenContent?.tokenWrapper?.token

        return AuthTokenWrapper.Builder().apply {
            this.clientKey = clientKey
            this.deviceIntegrityWrapper = DeviceIntegrityWrapper.Builder().apply {
                this.deviceIntegrityToken = deviceIntegrityToken ?: ByteString.EMPTY
                this.creationTime = makeTimestamp(System.currentTimeMillis())
            }.build()
        }.build()
    } catch (e: Exception) {
        Log.d(TAG, "regenerateToken: error ", e)
        return AuthTokenWrapper()
    }
}

private suspend fun requestDroidGuardSessionToken(context: Context, authToken: String): TokenResponse {
    val tokenWrapper = TokenRequestWrapper.Builder().apply {
        request = mutableListOf(TokenRequest.Builder().apply {
            droidGuardBody = DroidGuardBody.Builder().apply {
                tokenBody = DroidGuardSessionTokenContent()
            }.build()
        }.build())
    }.build()
    return requestExpressSyncData(context, authToken, tokenWrapper)
}

private suspend fun requestDeviceIntegrityToken(
    context: Context, authToken: String, session: String, token: ByteString
): TokenResponse {
    val tokenWrapper = TokenRequestWrapper.Builder().apply {
        request = mutableListOf(TokenRequest.Builder().apply {
            droidGuardBody = DroidGuardBody.Builder().apply {
                deviceBody = DeviceIntegrityTokenContent.Builder().apply {
                    sessionWrapper = SessionWrapper.Builder().apply {
                        type = KEY_DROID_GUARD_SESSION_TOKEN_V1
                        this.session = Session.Builder().apply {
                            id = session
                        }.build()
                    }.build()
                    this.token = token.utf8()
                    flowName = EXPRESS_INTEGRITY_FLOW_NAME
                }.build()
            }.build()
        }.build())
    }.build()
    return requestExpressSyncData(context, authToken, tokenWrapper)
}

// TODO: deduplicate with vending/extensions.kt
suspend fun getAuthToken(context: Context, authTokenType: String): String {
    val accountManager = AccountManager.get(context)
    val accounts = accountManager.getAccountsByType(DEFAULT_ACCOUNT_TYPE)
    var oauthToken = ""
    if (accounts.isEmpty()) {
        Log.w(TAG, "getAuthToken: No Google account found")
    } else for (account in accounts) {
        val result = suspendCoroutine { continuation ->
            accountManager.getAuthToken(account, authTokenType, false, { future: AccountManagerFuture<Bundle> ->
                try {
                    val result = future.result.getString(AccountManager.KEY_AUTHTOKEN)
                    continuation.resume(result)
                } catch (e: Exception) {
                    Log.w(TAG, "getAuthToken: ", e)
                    continuation.resume(null)
                }
            }, null)
        }
        if (result != null) {
            oauthToken = result
            break
        }
    }
    return oauthToken
}

suspend fun requestIntegritySyncData(context: Context, authToken: String, request: IntegrityRequest): IntegrityResponse {
    val androidId = GServices.getString(context.contentResolver, "android_id", "1")?.toLong() ?: 1
    return HttpClient().post(
        url = "https://play-fe.googleapis.com/fdfe/integrity",
        headers = buildRequestHeaders(authToken, androidId),
        payload = request,
        adapter = IntegrityResponse.ADAPTER
    )
}

suspend fun requestExpressSyncData(context: Context, authToken: String, request: TokenRequestWrapper): TokenResponse {
    val androidId = GServices.getString(context.contentResolver, "android_id", "1")?.toLong() ?: 1
    return HttpClient().post(
        url = "https://play-fe.googleapis.com/fdfe/sync?nocache_qos=lt",
        headers = buildRequestHeaders(authToken, androidId),
        payload = request,
        adapter = TokenResponse.ADAPTER
    )
}

suspend fun requestIntermediateIntegrity(
    context: Context, authToken: String, request: IntermediateIntegrityRequest
): IntermediateIntegrityResponseWrapperExtend {
    val androidId = GServices.getString(context.contentResolver, "android_id", "1")?.toLong() ?: 1
    return HttpClient().post(
        url = "https://play-fe.googleapis.com/fdfe/intermediateIntegrity",
        headers = buildRequestHeaders(authToken, androidId),
        payload = request,
        adapter = IntermediateIntegrityResponseWrapperExtend.ADAPTER
    )
}
