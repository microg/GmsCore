/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.expressintegrityservice

import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.android.vending.AUTH_TOKEN_SCOPE
import com.android.vending.VendingPreferences
import com.android.vending.makeTimestamp
import com.google.android.finsky.AuthTokenWrapper
import com.google.android.finsky.ClientKey
import com.google.android.finsky.DeviceIntegrityWrapper
import com.google.android.finsky.ExpressIntegrityResponse
import com.google.android.finsky.INTERMEDIATE_INTEGRITY_HARD_EXPIRATION
import com.google.android.finsky.IntegrityAdvice
import com.google.android.finsky.IntermediateIntegrityRequest
import com.google.android.finsky.IntermediateIntegrityResponse
import com.google.android.finsky.IntermediateIntegritySession
import com.google.android.finsky.KEY_CLOUD_PROJECT
import com.google.android.finsky.KEY_ERROR
import com.google.android.finsky.KEY_NONCE
import com.google.android.finsky.KEY_PACKAGE_NAME
import com.google.android.finsky.KEY_REQUEST_MODE
import com.google.android.finsky.KEY_REQUEST_TOKEN_SID
import com.google.android.finsky.KEY_REQUEST_VERDICT_OPT_OUT
import com.google.android.finsky.KEY_TOKEN
import com.google.android.finsky.KEY_WARM_UP_SID
import com.google.android.finsky.PlayProtectDetails
import com.google.android.finsky.PlayProtectState
import com.google.android.finsky.RESULT_UN_AUTH
import com.google.android.finsky.RequestMode
import com.google.android.finsky.TestErrorType
import com.google.android.finsky.buildClientKeyExtend
import com.google.android.finsky.buildInstallSourceMetaData
import com.google.android.finsky.callerAppToIntegrityData
import com.google.android.finsky.encodeBase64
import com.google.android.finsky.ensureContainsLockBootloader
import com.google.android.finsky.getAuthToken
import com.google.android.finsky.getExpirationTime
import com.google.android.finsky.getIntegrityRequestWrapper
import com.google.android.finsky.getPackageInfoCompat
import com.google.android.finsky.getPlayCoreVersion
import com.google.android.finsky.isNetworkConnected
import com.google.android.finsky.md5
import com.google.android.finsky.model.IntegrityErrorCode
import com.google.android.finsky.model.StandardIntegrityException
import com.google.android.finsky.readAes128GcmBuilderFromClientKey
import com.google.android.finsky.requestIntermediateIntegrity
import com.google.android.finsky.sha256
import com.google.android.finsky.signaturesCompat
import com.google.android.finsky.updateAppIntegrityContent
import com.google.android.finsky.updateExpressAuthTokenWrapper
import com.google.android.finsky.updateExpressClientKey
import com.google.android.finsky.updateExpressSessionTime
import com.google.android.finsky.updateLocalExpressFilePB
import com.google.android.finsky.validateIntermediateIntegrityResponse
import com.google.android.play.core.integrity.protocol.IExpressIntegrityService
import com.google.android.play.core.integrity.protocol.IExpressIntegrityServiceCallback
import com.google.android.play.core.integrity.protocol.IRequestDialogCallback
import com.google.crypto.tink.config.TinkConfig
import okio.ByteString.Companion.toByteString
import org.microg.gms.profile.ProfileManager
import org.microg.gms.vending.PlayIntegrityData
import org.microg.vending.billing.DEFAULT_ACCOUNT_TYPE
import org.microg.vending.proto.Timestamp
import kotlin.random.Random

private const val TAG = "ExpressIntegrityService"

class ExpressIntegrityService : LifecycleService() {

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        ProfileManager.ensureInitialized(this)
        Log.d(TAG, "onBind")
        TinkConfig.register()
        return ExpressIntegrityServiceImpl(this, lifecycle).asBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind")
        return super.onUnbind(intent)
    }
}

private class ExpressIntegrityServiceImpl(private val context: Context, override val lifecycle: Lifecycle) : IExpressIntegrityService.Stub(), LifecycleOwner {

    private var visitData: PlayIntegrityData? = null

    override fun warmUpIntegrityToken(bundle: Bundle, callback: IExpressIntegrityServiceCallback?) {
        lifecycleScope.launchWhenCreated {
            runCatching {
                val callingPackageName = bundle.getString(KEY_PACKAGE_NAME)
                if (callingPackageName == null) {
                    throw StandardIntegrityException(IntegrityErrorCode.INTERNAL_ERROR, "Null packageName.")
                }
                visitData = callerAppToIntegrityData(context, callingPackageName)
                if (visitData?.allowed != true) {
                    throw StandardIntegrityException(IntegrityErrorCode.API_NOT_AVAILABLE, "Not allowed visit")
                }
                val playIntegrityEnabled = VendingPreferences.isDeviceAttestationEnabled(context)
                if (!playIntegrityEnabled) {
                    throw StandardIntegrityException(IntegrityErrorCode.API_NOT_AVAILABLE, "API is disabled")
                }

                if (!context.isNetworkConnected()) {
                    throw StandardIntegrityException(IntegrityErrorCode.NETWORK_ERROR, "No network is available")
                }

                val expressIntegritySession = ExpressIntegritySession(
                    packageName = callingPackageName ?: "",
                    cloudProjectNumber = bundle.getLong(KEY_CLOUD_PROJECT, 0L),
                    sessionId = Random.nextLong(),
                    null,
                    0,
                    null,
                    webViewRequestMode = bundle.getInt(KEY_REQUEST_MODE, 0)
                )
                Log.d(TAG, "warmUpIntegrityToken session:$expressIntegritySession}")

                updateExpressSessionTime(context, expressIntegritySession, refreshWarmUpMethodTime = true, refreshRequestMethodTime = false)

                val clientKey = updateExpressClientKey(context)

                val authToken = getAuthToken(context, AUTH_TOKEN_SCOPE)
                if (TextUtils.isEmpty(authToken)) {
                    Log.w(TAG, "warmUpIntegrityToken: Got null auth token for type: $AUTH_TOKEN_SCOPE")
                }
                Log.d(TAG, "warmUpIntegrityToken authToken: $authToken")

                val expressFilePB = updateExpressAuthTokenWrapper(context, expressIntegritySession, authToken, clientKey)

                val tokenWrapper = expressFilePB.tokenWrapper ?: AuthTokenWrapper()
                val tokenClientKey = tokenWrapper.clientKey ?: ClientKey()
                val deviceIntegrityWrapper = tokenWrapper.deviceIntegrityWrapper ?: DeviceIntegrityWrapper()
                val creationTime = tokenWrapper.deviceIntegrityWrapper?.creationTime ?: Timestamp()
                val lastManualSoftRefreshTime = tokenWrapper.lastManualSoftRefreshTime ?: Timestamp()

                val deviceIntegrityAndExpiredKey = DeviceIntegrityAndExpiredKey(
                    deviceIntegrity = DeviceIntegrity(
                        tokenClientKey, deviceIntegrityWrapper.deviceIntegrityToken, creationTime, lastManualSoftRefreshTime
                    ), expressFilePB.expiredDeviceKey ?: ClientKey()
                )

                val deviceIntegrity = deviceIntegrityAndExpiredKey.deviceIntegrity
                if (deviceIntegrity.deviceIntegrityToken?.size == 0 || deviceIntegrity.clientKey?.keySetHandle?.size == 0) {
                    throw StandardIntegrityException("DroidGuard token is empty.")
                }

                val deviceKeyMd5 = Base64.encodeToString(
                    deviceIntegrity.clientKey?.keySetHandle?.md5()?.toByteArray(), Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE
                )
                if (deviceKeyMd5.isNullOrEmpty()) {
                    throw StandardIntegrityException("Null deviceKeyMd5.")
                }

                val deviceIntegrityResponse = DeviceIntegrityResponse(
                    deviceIntegrity, false, deviceKeyMd5, deviceIntegrityAndExpiredKey.expiredDeviceKey
                )

                val packageInfo = context.packageManager.getPackageInfoCompat(
                    expressIntegritySession.packageName, PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_SIGNATURES
                )
                val certificateSha256Hashes = packageInfo.signaturesCompat.map {
                    it.toByteArray().sha256().encodeBase64(noPadding = true, noWrap = true, urlSafe = true)
                }

                val packageInformation = PackageInformation(certificateSha256Hashes, packageInfo.versionCode)
                val clientKeyExtend = buildClientKeyExtend(context, expressIntegritySession, packageInformation, clientKey)
                val intermediateIntegrityRequest = IntermediateIntegrityRequest.Builder().apply {
                    deviceIntegrityToken(deviceIntegrityResponse.deviceIntegrity.deviceIntegrityToken)
                    readAes128GcmBuilderFromClientKey(deviceIntegrityResponse.deviceIntegrity.clientKey)?.let {
                        clientKeyExtendBytes(it.encrypt(clientKeyExtend.encode(), null).toByteString())
                    }
                    playCoreVersion(bundle.getPlayCoreVersion())
                    sessionId(expressIntegritySession.sessionId)
                    installSourceMetaData(buildInstallSourceMetaData(context, expressIntegritySession.packageName))
                    cloudProjectNumber(expressIntegritySession.cloudProjectNumber)
                    playProtectDetails(PlayProtectDetails(PlayProtectState.PLAY_PROTECT_STATE_NONE))
                    if (expressIntegritySession.webViewRequestMode != 0) {
                        requestMode(RequestMode.Builder().mode(expressIntegritySession.webViewRequestMode.takeIf { it in 0..2 } ?: 0).build())
                    }
                }.build()

                Log.d(TAG, "intermediateIntegrityRequest: $intermediateIntegrityRequest")

                val intermediateIntegrityResponse = requestIntermediateIntegrity(context, authToken, intermediateIntegrityRequest).intermediateIntegrityResponseWrapper?.intermediateIntegrityResponse
                    ?: IntermediateIntegrityResponse()

                Log.d(TAG, "requestIntermediateIntegrity response: ${intermediateIntegrityResponse.encode().encodeBase64(true)}")

                val errorCode = intermediateIntegrityResponse.errorInfo?.let { error ->
                    if (error.errorCode == null) {
                        null
                    } else if (error.testErrorType == TestErrorType.REQUEST_EXPRESS) {
                        error.errorCode
                    } else if (error.testErrorType == TestErrorType.WARMUP) {
                        throw StandardIntegrityException(error.errorCode, "Server-specified exception")
                    } else null
                }

                val defaultAccountName: String = runCatching {
                    if (expressIntegritySession.webViewRequestMode != 0) {
                        RESULT_UN_AUTH
                    } else {
                        AccountManager.get(context).getAccountsByType(DEFAULT_ACCOUNT_TYPE).firstOrNull()?.name ?: RESULT_UN_AUTH
                    }
                }.getOrDefault(RESULT_UN_AUTH)

                val callerKeyMd5 = clientKey.encode().md5() ?: throw StandardIntegrityException("Null callerKeyMd5")
                val refreshClientKey = clientKey.newBuilder()
                    .generated(makeTimestamp(System.currentTimeMillis()))
                    .build()
                val fixedAdvice = IntegrityAdvice.Builder()
                    .advices(intermediateIntegrityResponse.integrityAdvice?.advices.ensureContainsLockBootloader())
                    .build()
                val intermediateIntegrityResponseData = IntermediateIntegrityResponseData(
                    intermediateIntegrity = IntermediateIntegrity(
                        expressIntegritySession.packageName,
                        expressIntegritySession.cloudProjectNumber,
                        defaultAccountName,
                        refreshClientKey,
                        intermediateIntegrityResponse.intermediateToken,
                        intermediateIntegrityResponse.serverGenerated,
                        expressIntegritySession.webViewRequestMode,
                        errorCode,
                        fixedAdvice
                    ),
                    callerKeyMd5 = callerKeyMd5.encodeBase64(noPadding = true),
                    appVersionCode = packageInformation.versionCode,
                    deviceIntegrityResponse = deviceIntegrityResponse,
                    appAccessRiskVerdictEnabled = intermediateIntegrityResponse.appAccessRiskVerdictEnabled
                )

                validateIntermediateIntegrityResponse(intermediateIntegrityResponseData)

                updateLocalExpressFilePB(context, intermediateIntegrityResponseData)

                visitData?.updateAppIntegrityContent(context, System.currentTimeMillis(), "$TAG visited success.", true)
                callback?.onWarmResult(bundleOf(KEY_WARM_UP_SID to expressIntegritySession.sessionId))
            }.onFailure {
                val exception = it as? StandardIntegrityException ?: StandardIntegrityException(it.message)
                Log.w(TAG, "warm up has failed: code=${exception.code}, message=${exception.message}", exception)
                visitData?.updateAppIntegrityContent(context, System.currentTimeMillis(), "$TAG visited failed. ${exception.message}")
                callback?.onWarmResult(bundleOf(KEY_ERROR to exception.code))
            }
        }
    }

    override fun requestExpressIntegrityToken(bundle: Bundle, callback: IExpressIntegrityServiceCallback?) {
        Log.d(TAG, "requestExpressIntegrityToken bundle:$bundle")
        lifecycleScope.launchWhenCreated {
            runCatching {
                val callingPackageName = bundle.getString(KEY_PACKAGE_NAME)
                if (callingPackageName == null) {
                    throw StandardIntegrityException(IntegrityErrorCode.INTERNAL_ERROR, "Null packageName.")
                }
                visitData = callerAppToIntegrityData(context, callingPackageName)
                if (visitData?.allowed != true) {
                    throw StandardIntegrityException(IntegrityErrorCode.API_NOT_AVAILABLE, "Not allowed visit")
                }
                val playIntegrityEnabled = VendingPreferences.isDeviceAttestationEnabled(context)
                if (!playIntegrityEnabled) {
                    throw StandardIntegrityException(IntegrityErrorCode.API_NOT_AVAILABLE, "API is disabled")
                }

                val expressIntegritySession = ExpressIntegritySession(
                    packageName = callingPackageName,
                    cloudProjectNumber = bundle.getLong(KEY_CLOUD_PROJECT, 0L),
                    sessionId = Random.nextLong(),
                    requestHash = bundle.getString(KEY_NONCE),
                    originatingWarmUpSessionId = bundle.getLong(KEY_WARM_UP_SID, 0),
                    verdictOptOut = bundle.getIntegerArrayList(KEY_REQUEST_VERDICT_OPT_OUT),
                    webViewRequestMode = bundle.getInt(KEY_REQUEST_MODE, 0)
                )

                Log.d(TAG, "requestExpressIntegrityToken session:$expressIntegritySession}")

                if (TextUtils.isEmpty(expressIntegritySession.packageName)) {
                    Log.w(TAG, "packageName is empty.")
                    callback?.onRequestResult(bundleOf(KEY_ERROR to IntegrityErrorCode.INTERNAL_ERROR))
                    return@launchWhenCreated
                }

                if (expressIntegritySession.cloudProjectNumber <= 0L) {
                    Log.w(TAG, "cloudProjectVersion error")
                    callback?.onRequestResult(bundleOf(KEY_ERROR to IntegrityErrorCode.CLOUD_PROJECT_NUMBER_IS_INVALID))
                    return@launchWhenCreated
                }

                if (expressIntegritySession.requestHash?.length!! > 500) {
                    Log.w(TAG, "requestHash error")
                    callback?.onRequestResult(bundleOf(KEY_ERROR to IntegrityErrorCode.REQUEST_HASH_TOO_LONG))
                    return@launchWhenCreated
                }

                updateExpressSessionTime(context, expressIntegritySession, refreshWarmUpMethodTime = false, refreshRequestMethodTime = true)

                val defaultAccountName: String = runCatching {
                    if (expressIntegritySession.webViewRequestMode != 0) {
                        RESULT_UN_AUTH
                    } else {
                        AccountManager.get(context).getAccountsByType(DEFAULT_ACCOUNT_TYPE).firstOrNull()?.name ?: RESULT_UN_AUTH
                    }
                }.getOrDefault(RESULT_UN_AUTH)

                val integrityRequestWrapper = getIntegrityRequestWrapper(context, expressIntegritySession, defaultAccountName)
                if (integrityRequestWrapper == null) {
                    Log.w(TAG, "integrityRequestWrapper is null")
                    callback?.onRequestResult(bundleOf(KEY_ERROR to IntegrityErrorCode.INTEGRITY_TOKEN_PROVIDER_INVALID))
                    return@launchWhenCreated
                }

                integrityRequestWrapper.deviceIntegrityWrapper?.errorCode?.let {
                    throw StandardIntegrityException(it, "Server-specified exception")
                }
                val expirationTime = integrityRequestWrapper.getExpirationTime()

                if (expirationTime > INTERMEDIATE_INTEGRITY_HARD_EXPIRATION * 1000) {
                    Log.w(TAG, "Intermediate integrity hard expiration reached.")
                    callback?.onRequestResult(bundleOf(KEY_ERROR to IntegrityErrorCode.INTEGRITY_TOKEN_PROVIDER_INVALID))
                    return@launchWhenCreated
                }
                Log.d(TAG, "Intermediate integrity token generated time $expirationTime.")

                val integritySession = IntermediateIntegritySession.Builder().creationTime(makeTimestamp(System.currentTimeMillis())).requestHash(expressIntegritySession.requestHash)
                    .sessionId(Random.nextBytes(8).toByteString()).timestampMillis(expirationTime.toInt()).build()

                val expressIntegrityResponse = ExpressIntegrityResponse.Builder().apply {
                    this.deviceIntegrityToken = integrityRequestWrapper.deviceIntegrityWrapper?.deviceIntegrityToken
                    this.sessionHashAes128 = readAes128GcmBuilderFromClientKey(integrityRequestWrapper.callerKey)?.encrypt(
                        integritySession.encode(), null
                    )?.toByteString()
                }.build()

                val token = Base64.encodeToString(
                    expressIntegrityResponse.encode(), Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE
                )

                Log.d(TAG, "requestExpressIntegrityToken token: $token, sid: ${expressIntegritySession.sessionId}, mode: ${expressIntegritySession.webViewRequestMode}")
                visitData?.updateAppIntegrityContent(context, System.currentTimeMillis(), "$TAG visited success.", true)
                callback?.onRequestResult(
                    bundleOf(
                        KEY_TOKEN to token,
                        KEY_REQUEST_TOKEN_SID to expressIntegritySession.sessionId,
                        KEY_REQUEST_MODE to expressIntegritySession.webViewRequestMode
                    )
                )
            }.onFailure {
                val exception = it as? StandardIntegrityException ?: StandardIntegrityException(it.message)
                Log.w(TAG, "requesting token has failed: code=${exception.code}, message=${exception.message}", exception)
                visitData?.updateAppIntegrityContent(context, System.currentTimeMillis(), "$TAG visited failed. ${exception.message}")
                callback?.onRequestResult(bundleOf(KEY_ERROR to exception.code))
            }
        }
    }

    override fun requestAndShowDialog(bundle: Bundle?, callback: IRequestDialogCallback?) {
        Log.d(TAG, "requestAndShowDialog bundle:$bundle")
        callback?.onRequestDialog(bundleOf(KEY_ERROR to IntegrityErrorCode.INTERNAL_ERROR))
    }

}

private fun IExpressIntegrityServiceCallback.onWarmResult(result: Bundle) {
    if (asBinder()?.isBinderAlive == false) {
        Log.e(TAG, "onWarmResult IExpressIntegrityServiceCallback Binder died")
        return
    }
    Log.d(TAG, "IExpressIntegrityServiceCallback onWarmResult success: $result")
    try {
        onWarmUpExpressIntegrityToken(result)
    } catch (e: Exception) {
        Log.w(TAG, "error -> $e")
    }
}

private fun IExpressIntegrityServiceCallback.onRequestResult(result: Bundle) {
    if (asBinder()?.isBinderAlive == false) {
        Log.e(TAG, "onRequestResult IExpressIntegrityServiceCallback Binder died")
        return
    }
    Log.d(TAG, "IExpressIntegrityServiceCallback onRequestResult success: $result")
    try {
        onRequestExpressIntegrityToken(result)
    } catch (e: Exception) {
        Log.w(TAG, "error -> $e")
    }
}