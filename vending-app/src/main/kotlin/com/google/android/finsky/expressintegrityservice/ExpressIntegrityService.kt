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
import android.os.RemoteException
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.android.vending.AUTH_TOKEN_SCOPE
import com.android.vending.Timestamp
import com.android.vending.makeTimestamp
import com.android.volley.AuthFailureError
import com.google.android.finsky.AuthTokenWrapper
import com.google.android.finsky.ClientKey
import com.google.android.finsky.ClientKeyExtend
import com.google.android.finsky.DeviceIntegrityWrapper
import com.google.android.finsky.ExpressIntegrityResponse
import com.google.android.finsky.IntermediateIntegrityRequest
import com.google.android.finsky.IntermediateIntegritySession
import com.google.android.finsky.KEY_CLOUD_PROJECT
import com.google.android.finsky.KEY_NONCE
import com.google.android.finsky.KEY_OPT_PACKAGE
import com.google.android.finsky.KEY_PACKAGE_NAME
import com.google.android.finsky.KEY_REQUEST_MODE
import com.google.android.finsky.KEY_ERROR
import com.google.android.finsky.KEY_REQUEST_TOKEN_SID
import com.google.android.finsky.KEY_REQUEST_VERDICT_OPT_OUT
import com.google.android.finsky.KEY_TOKEN
import com.google.android.finsky.KEY_WARM_UP_SID
import com.google.android.finsky.PlayProtectDetails
import com.google.android.finsky.PlayProtectState
import com.google.android.finsky.RESULT_UN_AUTH
import com.google.android.finsky.RequestMode
import com.google.android.finsky.buildPlayCoreVersion
import com.google.android.finsky.encodeBase64
import com.google.android.finsky.fetchCertificateChain
import com.google.android.finsky.getAuthToken
import com.google.android.finsky.getIntegrityRequestWrapper
import com.google.android.finsky.getPackageInfoCompat
import com.google.android.finsky.model.IntegrityErrorCode
import com.google.android.finsky.readAes128GcmBuilderFromClientKey
import com.google.android.finsky.requestIntermediateIntegrity
import com.google.android.finsky.sha256
import com.google.android.finsky.signaturesCompat
import com.google.android.finsky.updateExpressAuthTokenWrapper
import com.google.android.finsky.updateExpressClientKey
import com.google.android.finsky.updateExpressSessionTime
import com.google.android.finsky.updateLocalExpressFilePB
import com.google.android.play.core.integrity.protocol.IExpressIntegrityService
import com.google.android.play.core.integrity.protocol.IExpressIntegrityServiceCallback
import com.google.android.play.core.integrity.protocol.IRequestDialogCallback
import com.google.crypto.tink.config.TinkConfig
import okio.ByteString.Companion.toByteString
import org.microg.gms.profile.ProfileManager
import org.microg.vending.billing.DEFAULT_ACCOUNT_TYPE
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

    override fun warmUpIntegrityToken(bundle: Bundle, callback: IExpressIntegrityServiceCallback?) {
        lifecycleScope.launchWhenCreated {
            runCatching {
                val authToken = getAuthToken(context, AUTH_TOKEN_SCOPE)
                if (TextUtils.isEmpty(authToken)) {
                    Log.w(TAG, "warmUpIntegrityToken: Got null auth token for type: $AUTH_TOKEN_SCOPE")
                }
                Log.d(TAG, "warmUpIntegrityToken authToken: $authToken")

                val expressIntegritySession = ExpressIntegritySession(
                    packageName = bundle.getString(KEY_PACKAGE_NAME) ?: "",
                    cloudProjectVersion = bundle.getLong(KEY_CLOUD_PROJECT, 0L),
                    sessionId = Random.nextLong(),
                    null,
                    0,
                    null,
                    webViewRequestMode = bundle.getInt(KEY_REQUEST_MODE, 0)
                )
                updateExpressSessionTime(context, expressIntegritySession, refreshWarmUpMethodTime = true, refreshRequestMethodTime = false)

                val clientKey = updateExpressClientKey(context)
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
                    throw RuntimeException("DroidGuard token is empty.")
                }

                val deviceKeyMd5 = Base64.encodeToString(
                    deviceIntegrity.clientKey?.keySetHandle?.md5()?.toByteArray(), Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE
                )
                if (deviceKeyMd5.isNullOrEmpty()) {
                    throw RuntimeException("Null deviceKeyMd5.")
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

                val clientKeyExtend = ClientKeyExtend.Builder().apply {
                    cloudProjectNumber = expressIntegritySession.cloudProjectVersion
                    keySetHandle = clientKey.keySetHandle
                    if (expressIntegritySession.webViewRequestMode == 2) {
                        this.optPackageName = KEY_OPT_PACKAGE
                        this.versionCode = 0
                    } else {
                        this.optPackageName = expressIntegritySession.packageName
                        this.versionCode = packageInformation.versionCode
                        this.certificateSha256Hashes = packageInformation.certificateSha256Hashes
                    }
                }.build()

                val certificateChainList = fetchCertificateChain(context, clientKeyExtend.keySetHandle?.sha256()?.toByteArray())

                val sessionId = expressIntegritySession.sessionId
                val playCoreVersion = bundle.buildPlayCoreVersion()

                Log.d(TAG, "warmUpIntegrityToken sessionId:$sessionId")

                val intermediateIntegrityRequest = IntermediateIntegrityRequest.Builder().apply {
                    deviceIntegrityToken(deviceIntegrityResponse.deviceIntegrity.deviceIntegrityToken)
                    readAes128GcmBuilderFromClientKey(deviceIntegrityResponse.deviceIntegrity.clientKey)?.let {
                        clientKeyExtendBytes(it.encrypt(clientKeyExtend.encode(), null).toByteString())
                    }
                    playCoreVersion(playCoreVersion)
                    sessionId(sessionId)
                    certificateChainWrapper(IntermediateIntegrityRequest.CertificateChainWrapper(certificateChainList))
                    playProtectDetails(PlayProtectDetails(PlayProtectState.PLAY_PROTECT_STATE_NONE))
                    if (expressIntegritySession.webViewRequestMode != 0) {
                        requestMode(RequestMode.Builder().mode(expressIntegritySession.webViewRequestMode.takeIf { it in 0..2 } ?: 0).build())
                    }
                }.build()

                Log.d(TAG, "intermediateIntegrityRequest: $intermediateIntegrityRequest")

                val intermediateIntegrityResponse = requestIntermediateIntegrity(context, authToken, intermediateIntegrityRequest).intermediateIntegrityResponseWrapper?.intermediateIntegrityResponse
                    ?: throw RuntimeException("intermediateIntegrityResponse is null.")

                Log.d(TAG, "requestIntermediateIntegrity: $intermediateIntegrityResponse")

                val defaultAccountName: String = runCatching {
                    if (expressIntegritySession.webViewRequestMode != 0) {
                        RESULT_UN_AUTH
                    } else {
                        AccountManager.get(context).getAccountsByType(DEFAULT_ACCOUNT_TYPE).firstOrNull()?.name ?: RESULT_UN_AUTH
                    }
                }.getOrDefault(RESULT_UN_AUTH)

                val intermediateIntegrityResponseData = IntermediateIntegrityResponseData(
                    intermediateIntegrity = IntermediateIntegrity(
                        expressIntegritySession.packageName,
                        expressIntegritySession.cloudProjectVersion,
                        defaultAccountName,
                        clientKey,
                        intermediateIntegrityResponse.intermediateToken,
                        intermediateIntegrityResponse.serverGenerated,
                        expressIntegritySession.webViewRequestMode,
                        0
                    ),
                    callerKeyMd5 = Base64.encodeToString(
                        clientKey.encode(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
                    ),
                    appVersionCode = packageInformation.versionCode,
                    deviceIntegrityResponse = deviceIntegrityResponse,
                    appAccessRiskVerdictEnabled = intermediateIntegrityResponse.appAccessRiskVerdictEnabled
                )

                updateLocalExpressFilePB(context, intermediateIntegrityResponseData)

                callback?.onWarmResult(bundleOf(KEY_WARM_UP_SID to sessionId))
            }.onFailure {
                callback?.onWarmResult(bundleOf(KEY_ERROR to IntegrityErrorCode.INTEGRITY_TOKEN_PROVIDER_INVALID))
            }
        }
    }

    override fun requestExpressIntegrityToken(bundle: Bundle, callback: IExpressIntegrityServiceCallback?) {
        Log.d(TAG, "requestExpressIntegrityToken bundle:$bundle")
        lifecycleScope.launchWhenCreated {
            val expressIntegritySession = ExpressIntegritySession(
                packageName = bundle.getString(KEY_PACKAGE_NAME) ?: "",
                cloudProjectVersion = bundle.getLong(KEY_CLOUD_PROJECT, 0L),
                sessionId = Random.nextLong(),
                requestHash = bundle.getString(KEY_NONCE),
                originatingWarmUpSessionId = bundle.getLong(KEY_WARM_UP_SID, 0),
                verdictOptOut = bundle.getIntegerArrayList(KEY_REQUEST_VERDICT_OPT_OUT),
                webViewRequestMode = bundle.getInt(KEY_REQUEST_MODE, 0)
            )

            if (TextUtils.isEmpty(expressIntegritySession.packageName)) {
                Log.w(TAG, "packageName is empty.")
                callback?.onRequestResult(bundleOf(KEY_ERROR to IntegrityErrorCode.INTERNAL_ERROR))
                return@launchWhenCreated
            }

            if (expressIntegritySession.cloudProjectVersion <= 0L) {
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

            try {
                val integritySession = IntermediateIntegritySession.Builder().creationTime(makeTimestamp(System.currentTimeMillis())).requestHash(expressIntegritySession.requestHash)
                    .sessionId(Random.nextBytes(8).toByteString()).timestampMillis(0).build()

                val expressIntegrityResponse = ExpressIntegrityResponse.Builder().apply {
                    this.deviceIntegrityToken = integrityRequestWrapper.deviceIntegrityWrapper?.deviceIntegrityToken
                    this.sessionHashAes128 = readAes128GcmBuilderFromClientKey(integrityRequestWrapper.callerKey)?.encrypt(
                        integritySession.encode(), null
                    )?.toByteString()
                }.build()

                val token = Base64.encodeToString(
                    expressIntegrityResponse.encode(), Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE
                )

                callback?.onRequestResult(
                    bundleOf(
                        KEY_TOKEN to token,
                        KEY_REQUEST_TOKEN_SID to expressIntegritySession.sessionId,
                        KEY_REQUEST_MODE to expressIntegritySession.webViewRequestMode
                    )
                )
                Log.d(TAG, "requestExpressIntegrityToken token: $token, sid: ${expressIntegritySession.sessionId}, mode: ${expressIntegritySession.webViewRequestMode}")
            } catch (exception: RemoteException) {
                Log.e(TAG, "requesting token has failed for ${expressIntegritySession.packageName}.")
                callback?.onRequestResult(bundleOf(KEY_ERROR to IntegrityErrorCode.INTEGRITY_TOKEN_PROVIDER_INVALID))
            }
        }
    }

    override fun requestAndShowDialog(bundle: Bundle?, callback: IRequestDialogCallback?) {
        Log.d(TAG, "requestAndShowDialog bundle:$bundle")
        callback?.onRequestAndShowDialog(bundleOf(KEY_ERROR to IntegrityErrorCode.INTERNAL_ERROR))
    }

}

private fun IExpressIntegrityServiceCallback.onWarmResult(result: Bundle) {
    if (asBinder()?.isBinderAlive == false) {
        Log.e(TAG, "onWarmResult IExpressIntegrityServiceCallback Binder died")
        return
    }
    try {
        OnWarmUpIntegrityTokenCallback(result)
    } catch (e: Exception) {
        Log.w(TAG, "error -> $e")
    }
    Log.d(TAG, "IExpressIntegrityServiceCallback onWarmResult success: $result")
}

private fun IExpressIntegrityServiceCallback.onRequestResult(result: Bundle) {
    if (asBinder()?.isBinderAlive == false) {
        Log.e(TAG, "onRequestResult IExpressIntegrityServiceCallback Binder died")
        return
    }
    try {
        onRequestExpressIntegrityToken(result)
    } catch (e: Exception) {
        Log.w(TAG, "error -> $e")
    }
    Log.d(TAG, "IExpressIntegrityServiceCallback onRequestResult success: $result")
}