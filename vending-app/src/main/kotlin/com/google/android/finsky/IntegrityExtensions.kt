/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky

import android.accounts.AccountManager
import android.accounts.AccountManagerFuture
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.ConnectivityManager
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import com.android.vending.VendingPreferences
import com.android.vending.buildRequestHeaders
import com.android.vending.makeTimestamp
import com.google.android.finsky.expressintegrityservice.ExpressIntegritySession
import com.google.android.finsky.expressintegrityservice.IntermediateIntegrityResponseData
import com.google.android.finsky.expressintegrityservice.PackageInformation
import com.google.android.finsky.model.IntegrityErrorCode
import com.google.android.finsky.model.StandardIntegrityException
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
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.encode
import okio.ByteString.Companion.toByteString
import org.microg.gms.common.Constants
import org.microg.gms.profile.Build
import org.microg.gms.profile.ProfileManager
import org.microg.gms.utils.getFirstSignatureDigest
import org.microg.gms.vending.PlayIntegrityData
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
import java.security.NoSuchAlgorithmException
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
const val INTERMEDIATE_INTEGRITY_HARD_EXPIRATION = 86400L // 1 day
private const val TAG = "IntegrityExtensions"

fun callerAppToIntegrityData(context: Context, callingPackage: String): PlayIntegrityData {
    val pkgSignSha256ByteArray = context.packageManager.getFirstSignatureDigest(callingPackage, "SHA-256")
    if (pkgSignSha256ByteArray == null) {
        throw StandardIntegrityException(IntegrityErrorCode.APP_NOT_INSTALLED, "$callingPackage signature is null")
    }
    val pkgSignSha256 = Base64.encodeToString(pkgSignSha256ByteArray, Base64.NO_WRAP)
    Log.d(TAG, "callerToVisitData $callingPackage pkgSignSha256: $pkgSignSha256")
    val playIntegrityAppList = VendingPreferences.getPlayIntegrityAppList(context)
    val loadDataSet = PlayIntegrityData.loadDataSet(playIntegrityAppList)
    if (loadDataSet.isEmpty() || loadDataSet.none { it.packageName == callingPackage && it.pkgSignSha256 == pkgSignSha256 }) {
        return PlayIntegrityData(true, callingPackage, pkgSignSha256, System.currentTimeMillis())
    }
    return loadDataSet.first { it.packageName == callingPackage && it.pkgSignSha256 == pkgSignSha256 }
}

fun PlayIntegrityData.updateAppIntegrityContent(context: Context, time: Long, result: String, status: Boolean = false) {
    val playIntegrityAppList = VendingPreferences.getPlayIntegrityAppList(context)
    val loadDataSet = PlayIntegrityData.loadDataSet(playIntegrityAppList)
    val dataSetString = PlayIntegrityData.updateDataSetString(loadDataSet, apply {
        lastTime = time
        lastResult = result
        lastStatus = status
    })
    VendingPreferences.setPlayIntegrityAppList(context, dataSetString)
}

fun IntegrityRequestWrapper.getExpirationTime() = runCatching {
    val creationTimeStamp = deviceIntegrityWrapper?.creationTime ?: Timestamp(0, 0)
    val creationTime = (creationTimeStamp.seconds ?: 0) * 1000 + (creationTimeStamp.nanos ?: 0) / 1_000_000
    val currentTimeStamp = makeTimestamp(System.currentTimeMillis())
    val currentTime = (currentTimeStamp.seconds ?: 0) * 1000 + (currentTimeStamp.nanos ?: 0) / 1_000_000
    return@runCatching currentTime - creationTime
}.getOrDefault(0)

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

fun Context.isNetworkConnected(): Boolean {
    return try {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.activeNetworkInfo?.isConnected == true
    } catch (_: RuntimeException) {
        false
    }
}

private fun getExpressFilePB(context: Context): ExpressFilePB {
    return runCatching { FileInputStream(context.getProtoFile()).use { input -> ExpressFilePB.ADAPTER.decode(input) } }
        .onFailure { Log.w(TAG, "Failed to read express cache ", it) }
        .getOrDefault(ExpressFilePB())
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
            val signingInfo = signingInfo ?: return emptyArray()
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        } else {
            @Suppress("DEPRECATION") signatures ?: emptyArray()
        }
    }

fun ByteArray.encodeBase64(noPadding: Boolean, noWrap: Boolean = true, urlSafe: Boolean = true): String {
    var flags = 0
    if (noPadding) flags = flags or Base64.NO_PADDING
    if (noWrap) flags = flags or Base64.NO_WRAP
    if (urlSafe) flags = flags or Base64.URL_SAFE
    return Base64.encodeToString(this, flags)
}

fun ByteArray.md5(): ByteArray? {
    return try {
        val md5 = MessageDigest.getInstance("MD5")
        md5.digest(this)
    } catch (e: NoSuchAlgorithmException) {
        null
    }
}

fun ByteArray.sha256(): ByteArray {
    return MessageDigest.getInstance("SHA-256").digest(this)
}

fun Bundle.getPlayCoreVersion() = PlayCoreVersion(
    major = getInt(KEY_VERSION_MAJOR, 0), minor = getInt(KEY_VERSION_MINOR, 0), patch = getInt(KEY_VERSION_PATCH, 0)
)

fun List<AdviceType>?.ensureContainsLockBootloader(): List<AdviceType> {
    if (isNullOrEmpty()) return listOf(AdviceType.LOCK_BOOTLOADER)
    return if (contains(AdviceType.LOCK_BOOTLOADER)) this else listOf(AdviceType.LOCK_BOOTLOADER) + this
}

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

suspend fun getIntegrityRequestWrapper(context: Context, expressIntegritySession: ExpressIntegritySession, accountName: String) = withContext(Dispatchers.IO) {
    fun getUpdatedWebViewRequestMode(webViewRequestMode: Int): Int {
        return when (webViewRequestMode) {
            in 0..2 -> webViewRequestMode + 1
            else -> 1
        }
    }
    val expressFilePB = getExpressFilePB(context)
    expressFilePB.integrityRequestWrapper.filter { item ->
        TextUtils.equals(item.packageName, expressIntegritySession.packageName) && item.cloudProjectNumber == expressIntegritySession.cloudProjectNumber && getUpdatedWebViewRequestMode(
            expressIntegritySession.webViewRequestMode
        ) == getUpdatedWebViewRequestMode(
            item.webViewRequestMode ?: 0
        )
    }.firstOrNull { item ->
        TextUtils.equals(item.accountName, accountName)
    }
}

fun fetchCertificateChain(context: Context, attestationChallenge: ByteArray?): List<ByteString> {
    if (android.os.Build.VERSION.SDK_INT >= 24) {
        val devicePropertiesAttestationIncluded = context.packageManager.hasSystemFeature("android.software.device_id_attestation")
        val keyGenParameterSpecBuilder =
            KeyGenParameterSpec.Builder("integrity.api.key.alias", KeyProperties.PURPOSE_SIGN).apply {
                this.setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                this.setDigests(KeyProperties.DIGEST_SHA512)
                if (devicePropertiesAttestationIncluded) {
                    this.setAttestationChallenge(attestationChallenge)
                }
                if (android.os.Build.VERSION.SDK_INT >= 31) {
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
    val intermediateIntegrity = intermediateIntegrityResponseData.intermediateIntegrity
    val expressFilePB = getExpressFilePB(context)

    val integrityResponseWrapper = IntegrityRequestWrapper.Builder().apply {
        accountName = intermediateIntegrity.accountName
        packageName = intermediateIntegrity.packageName
        cloudProjectNumber = intermediateIntegrity.cloudProjectNumber
        callerKey = intermediateIntegrity.callerKey
        webViewRequestMode = intermediateIntegrity.webViewRequestMode.takeIf { it in 0..2 } ?: 0
        deviceIntegrityWrapper = DeviceIntegrityWrapper.Builder().apply {
            creationTime = intermediateIntegrity.callerKey.generated
            serverGenerated = intermediateIntegrity.serverGenerated
            deviceIntegrityToken = intermediateIntegrity.intermediateToken
        }.build()
        intermediateIntegrity.integrityAdvice?.let { advice = it }
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

        val expressFilePB = getExpressFilePB(context)

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
    val expressFilePB = getExpressFilePB(context)
    val oldClientKey = expressFilePB.clientKey ?: ClientKey()
    val generated = makeTimestamp(System.currentTimeMillis())

    val oldGeneratedSec = oldClientKey.generated?.seconds ?: 0
    val newGeneratedSec = generated.seconds ?: 0

    val useOld = oldClientKey.keySetHandle?.size != 0 && oldGeneratedSec >= newGeneratedSec - TEMPORARY_DEVICE_KEY_VALIDITY

    val clientKey = if (useOld) {
        Log.d(TAG, "Using existing clientKey, not expired. oldGeneratedSec=$oldGeneratedSec newGeneratedSec=$newGeneratedSec")
        oldClientKey
    } else {
        Log.d(TAG, "Generating new clientKey. oldKeyValid=${oldClientKey.keySetHandle?.size != 0} expired=${oldGeneratedSec < newGeneratedSec - TEMPORARY_DEVICE_KEY_VALIDITY}")
        val keySetHandle = KeysetHandle.generateNew(AesGcmKeyManager.aes128GcmTemplate())
        val keyBytes = ByteArrayOutputStream().use { output ->
            CleartextKeysetHandle.write(keySetHandle, BinaryKeysetWriter.withOutputStream(output))
            output.toByteArray()
        }
        Log.d(TAG, "New clientKey generated at timestamp: ${generated.seconds}")
        ClientKey.Builder()
            .generated(generated)
            .keySetHandle(ByteBuffer.wrap(keyBytes).toByteString())
            .build()
    }

    val newExpressFilePB = expressFilePB.newBuilder().clientKey(clientKey).build()
    FileOutputStream(context.getProtoFile()).use { output -> ExpressFilePB.ADAPTER.encode(output, newExpressFilePB) }
    clientKey
}

suspend fun updateExpressAuthTokenWrapper(context: Context, expressIntegritySession: ExpressIntegritySession, authToken: String, clientKey: ClientKey) = withContext(Dispatchers.IO) {
    var expressFilePB = getExpressFilePB(context)

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
    Log.d(TAG, "regenerateToken authToken:$authToken, packageName:$packageName, clientKey:$clientKey")
    try {
        val prefs = context.getSharedPreferences("droid_guard_token_session_id", Context.MODE_PRIVATE)
        val droidGuardTokenSession = try {
            val droidGuardSessionTokenResponse = requestDroidGuardSessionToken(context, authToken)
            if (droidGuardSessionTokenResponse.tokenWrapper == null) {
                throw RuntimeException("regenerateToken droidGuardSessionTokenResponse.tokenWrapper is Empty!")
            }
            val droidGuardTokenType = droidGuardSessionTokenResponse.tokenWrapper.tokenContent?.tokenType?.firstOrNull { it.type?.toInt() == 5 }
                ?: throw RuntimeException("regenerateToken droidGuardTokenType is null!")
            val sessionId = droidGuardTokenType.tokenSessionWrapper?.wrapper?.sessionContent?.session?.id
            if (sessionId.isNullOrEmpty()) {
                throw RuntimeException("regenerateToken sessionId is null")
            }
            sessionId.also { prefs.edit { putString(packageName, it) } }
        } catch (e: Exception) {
            Log.d(TAG, "regenerateToken: error ", e)
            prefs.getString(packageName, null)
        }

        Log.d(TAG, "regenerateToken: sessionId: $droidGuardTokenSession")
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
            this.lastManualSoftRefreshTime = makeTimestamp(System.currentTimeMillis())
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

fun buildClientKeyExtend(
    context: Context,
    session: ExpressIntegritySession,
    packageInformation: PackageInformation,
    clientKey: ClientKey
): ClientKeyExtend {
    return ClientKeyExtend.Builder().apply {
        cloudProjectNumber = session.cloudProjectNumber
        keySetHandle = clientKey.keySetHandle
        if (session.webViewRequestMode == 2) {
            this.optPackageName = KEY_OPT_PACKAGE
            this.versionCode = 0
        } else {
            this.optPackageName = session.packageName
            this.versionCode = packageInformation.versionCode
            this.certificateSha256Hashes = packageInformation.certificateSha256Hashes
        }
        this.deviceSerialHash = ProfileManager.getSerial(context).toByteArray().sha256().toByteString()
    }.build()
}

fun buildInstallSourceMetaData(
    context: Context,
    packageName: String
): InstallSourceMetaData {
    fun resolveInstallerType(name: String?): InstallerType = when {
        name.isNullOrEmpty() -> InstallerType.UNSPECIFIED_INSTALLER
        name == Constants.VENDING_PACKAGE_NAME -> InstallerType.PHONESKY_INSTALLER
        else -> InstallerType.OTHER_INSTALLER
    }

    fun resolvePackageSourceType(type: Int): PackageSourceType = when (type) {
        1 -> PackageSourceType.PACKAGE_SOURCE_OTHER
        2 -> PackageSourceType.PACKAGE_SOURCE_STORE
        3 -> PackageSourceType.PACKAGE_SOURCE_LOCAL_FILE
        4 -> PackageSourceType.PACKAGE_SOURCE_DOWNLOADED_FILE
        else -> PackageSourceType.PACKAGE_SOURCE_UNSPECIFIED
    }

    val builder = InstallSourceMetaData.Builder().apply {
        installingPackageName = InstallerType.UNSPECIFIED_INSTALLER
        initiatingPackageName = InstallerType.UNSPECIFIED_INSTALLER
        originatingPackageName = InstallerType.UNSPECIFIED_INSTALLER
        updateOwnerPackageName = InstallerType.UNSPECIFIED_INSTALLER
        packageSourceType = PackageSourceType.PACKAGE_SOURCE_UNSPECIFIED
    }

    val applicationInfo = runCatching {
        context.packageManager.getApplicationInfo(packageName, 0)
    }.getOrNull()

    if (Build.VERSION.SDK_INT >= 30) {
        runCatching {
            val info = context.packageManager.getInstallSourceInfo(packageName)
            builder.apply {
                initiatingPackageName = resolveInstallerType(info.initiatingPackageName)
                installingPackageName = resolveInstallerType(info.installingPackageName)
                originatingPackageName = resolveInstallerType(info.originatingPackageName)

                if (Build.VERSION.SDK_INT >= 34) {
                    updateOwnerPackageName = resolveInstallerType(info.updateOwnerPackageName)
                }
                if (Build.VERSION.SDK_INT >= 33) {
                    packageSourceType = resolvePackageSourceType(info.packageSource)
                }
            }
        }
    } else {
        builder.installingPackageName = runCatching {
            resolveInstallerType(context.packageManager.getInstallerPackageName(packageName))
        }.getOrElse { InstallerType.UNSPECIFIED_INSTALLER }
    }

    builder.appFlags = applicationInfo?.let { info ->
        buildList {
            if (info.flags and ApplicationInfo.FLAG_SYSTEM != 0) add(SystemAppFlag.FLAG_SYSTEM)
            if (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0) {
                add(SystemAppFlag.FLAG_UPDATED_SYSTEM_APP)
            }
        }.ifEmpty { listOf(SystemAppFlag.SYSTEM_APP_INFO_UNSPECIFIED) }
    } ?: listOf(SystemAppFlag.SYSTEM_APP_INFO_UNSPECIFIED)

    return builder.build()
}

fun validateIntermediateIntegrityResponse(intermediateIntegrityResponse: IntermediateIntegrityResponseData) {
    val intermediateIntegrity = intermediateIntegrityResponse.intermediateIntegrity

    requireNotNull(intermediateIntegrity.intermediateToken) { "Null intermediateToken" }
    requireNotNull(intermediateIntegrity.serverGenerated) { "Null serverGenerated" }
}

