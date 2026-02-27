/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.integrityservice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
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
import com.google.android.finsky.AccessibilityAbuseSignalDataWrapper
import com.google.android.finsky.AppAccessRiskDetailsResponse
import com.google.android.finsky.DisplayListenerMetadataWrapper
import com.google.android.finsky.INTEGRITY_FLOW_NAME
import com.google.android.finsky.INTEGRITY_PREFIX_ERROR
import com.google.android.finsky.InstalledAppsSignalDataWrapper
import com.google.android.finsky.IntegrityParams
import com.google.android.finsky.IntegrityRequest
import com.google.android.finsky.KEY_CLOUD_PROJECT
import com.google.android.finsky.KEY_NONCE
import com.google.android.finsky.KEY_PACKAGE_NAME
import com.google.android.finsky.PARAMS_BINDING_KEY
import com.google.android.finsky.PARAMS_GCP_N_KEY
import com.google.android.finsky.PARAMS_NONCE_SHA256_KEY
import com.google.android.finsky.PARAMS_PKG_KEY
import com.google.android.finsky.PARAMS_TM_S_KEY
import com.google.android.finsky.PARAMS_VC_KEY
import com.google.android.finsky.PackageNameWrapper
import com.google.android.finsky.PlayProtectDetails
import com.google.android.finsky.PlayProtectState
import com.google.android.finsky.SIGNING_FLAGS
import com.google.android.finsky.ScreenCaptureSignalDataWrapper
import com.google.android.finsky.ScreenOverlaySignalDataWrapper
import com.google.android.finsky.VersionCodeWrapper
import com.google.android.finsky.callerAppToIntegrityData
import com.google.android.finsky.getPlayCoreVersion
import com.google.android.finsky.encodeBase64
import com.google.android.finsky.getAuthToken
import com.google.android.finsky.getPackageInfoCompat
import com.google.android.finsky.model.IntegrityErrorCode
import com.google.android.finsky.model.StandardIntegrityException
import com.google.android.finsky.requestIntegritySyncData
import com.google.android.finsky.sha256
import com.google.android.finsky.signaturesCompat
import com.google.android.finsky.updateAppIntegrityContent
import com.google.android.gms.droidguard.DroidGuard
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest
import com.google.android.gms.tasks.await
import com.google.android.play.core.integrity.protocol.IIntegrityService
import com.google.android.play.core.integrity.protocol.IIntegrityServiceCallback
import com.google.android.play.core.integrity.protocol.IRequestDialogCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.toByteString
import org.microg.gms.profile.ProfileManager
import org.microg.gms.vending.PlayIntegrityData

private const val TAG = "IntegrityService"

class IntegrityService : LifecycleService() {

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        ProfileManager.ensureInitialized(this)
        Log.d(TAG, "onBind")
        return IntegrityServiceImpl(this, lifecycle).asBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind")
        return super.onUnbind(intent)
    }
}

private class IntegrityServiceImpl(private val context: Context, override val lifecycle: Lifecycle) : IIntegrityService.Stub(), LifecycleOwner {

    private var integrityData: PlayIntegrityData? = null

    override fun requestDialog(bundle: Bundle, callback: IRequestDialogCallback) {
        Log.d(TAG, "Method (requestDialog) called but not implemented ")
        requestAndShowDialog(bundle, callback)
    }

    override fun requestAndShowDialog(bundle: Bundle?, callback: IRequestDialogCallback?) {
        Log.d(TAG, "Not yet implemented: requestAndShowDialog")
    }

    override fun requestIntegrityToken(request: Bundle, callback: IIntegrityServiceCallback) {
        Log.d(TAG, "Method (requestIntegrityToken) called")
        lifecycleScope.launchWhenCreated {
            runCatching {
                val packageName = request.getString(KEY_PACKAGE_NAME)
                if (packageName == null) {
                    throw StandardIntegrityException(IntegrityErrorCode.INTERNAL_ERROR, "Null packageName.")
                }
                integrityData = callerAppToIntegrityData(context, packageName)
                if (integrityData?.allowed != true) {
                    throw StandardIntegrityException(IntegrityErrorCode.INTERNAL_ERROR, "Not allowed to request integrity token.")
                }
                val playIntegrityEnabled = VendingPreferences.isDeviceAttestationEnabled(context)
                if (!playIntegrityEnabled) {
                    throw StandardIntegrityException(IntegrityErrorCode.INTERNAL_ERROR, "API is disabled.")
                }
                val nonceArr = request.getByteArray(KEY_NONCE)
                if (nonceArr == null) {
                    throw StandardIntegrityException(IntegrityErrorCode.INTERNAL_ERROR, "Nonce missing.")
                }
                if (nonceArr.size < 16) {
                    throw StandardIntegrityException(IntegrityErrorCode.NONCE_TOO_SHORT, "Nonce too short.")
                }
                if (nonceArr.size >= 500) {
                    throw StandardIntegrityException(IntegrityErrorCode.NONCE_TOO_LONG, "Nonce too long.")
                }
                val cloudProjectNumber = request.getLong(KEY_CLOUD_PROJECT, 0L)
                val playCoreVersion = request.getPlayCoreVersion()
                Log.d(TAG, "requestIntegrityToken(packageName: $packageName, nonce: ${nonceArr.encodeBase64(false)}, cloudProjectNumber: $cloudProjectNumber, playCoreVersion: $playCoreVersion)")

                val packageInfo = context.packageManager.getPackageInfoCompat(packageName, SIGNING_FLAGS)
                val timestamp = makeTimestamp(System.currentTimeMillis())
                val versionCode = packageInfo.versionCode

                val integrityParams = IntegrityParams(
                    packageName = PackageNameWrapper(packageName),
                    versionCode = VersionCodeWrapper(versionCode),
                    nonce = nonceArr.encodeBase64(noPadding = false, noWrap = true, urlSafe = true),
                    certificateSha256Digests = packageInfo.signaturesCompat.map {
                        it.toByteArray().sha256().encodeBase64(noPadding = true, noWrap = true, urlSafe = true)
                    },
                    timestampAtRequest = timestamp,
                    cloudProjectNumber = cloudProjectNumber.takeIf { it > 0L }
                )

                val data = mutableMapOf(
                    PARAMS_PKG_KEY to packageName,
                    PARAMS_VC_KEY to versionCode.toString(),
                    PARAMS_NONCE_SHA256_KEY to nonceArr.sha256().encodeBase64(noPadding = true, noWrap = true, urlSafe = true),
                    PARAMS_TM_S_KEY to timestamp.seconds.toString(),
                    PARAMS_BINDING_KEY to integrityParams.encode().encodeBase64(noPadding = false, noWrap = true, urlSafe = true),
                )
                if (cloudProjectNumber > 0L) {
                    data[PARAMS_GCP_N_KEY] = cloudProjectNumber.toString()
                }

                var mapSize = 0
                data.entries.forEach { mapSize += it.key.toByteArray().size + it.value.toByteArray().size }
                if (mapSize > 65536) {
                    throw StandardIntegrityException(IntegrityErrorCode.INTERNAL_ERROR, "Content binding size exceeded maximum allowed size.")
                }

                val authToken = getAuthToken(context, AUTH_TOKEN_SCOPE)
                if (TextUtils.isEmpty(authToken)) {
                    Log.w(TAG, "requestIntegrityToken: Got null auth token for type: $AUTH_TOKEN_SCOPE")
                }
                Log.d(TAG, "requestIntegrityToken authToken: $authToken")

                val droidGuardData = withContext(Dispatchers.IO) {
                    val droidGuardResultsRequest = DroidGuardResultsRequest()
                    droidGuardResultsRequest.bundle.putString("thirdPartyCallerAppPackageName", packageName)
                    Log.d(TAG, "Running DroidGuard (flow: $INTEGRITY_FLOW_NAME, data: $data)")
                    val droidGuardToken = DroidGuard.getClient(context).getResults(INTEGRITY_FLOW_NAME, data, droidGuardResultsRequest).await()
                    Log.d(TAG, "Running DroidGuard (flow: $INTEGRITY_FLOW_NAME, droidGuardToken: $droidGuardToken)")
                    Base64.decode(droidGuardToken, Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE).toByteString()
                }

                if (droidGuardData.utf8().startsWith(INTEGRITY_PREFIX_ERROR)) {
                    Log.w(TAG, "droidGuardData: ${droidGuardData.utf8()}")
                    throw StandardIntegrityException(IntegrityErrorCode.NETWORK_ERROR, "DroidGuard failed.")
                }

                val integrityRequest = IntegrityRequest(
                    params = integrityParams,
                    flowName = INTEGRITY_FLOW_NAME,
                    droidGuardTokenRaw = droidGuardData,
                    playCoreVersion = playCoreVersion,
                    playProtectDetails = PlayProtectDetails(PlayProtectState.PLAY_PROTECT_STATE_NO_PROBLEMS),
                    appAccessRiskDetailsResponse = AppAccessRiskDetailsResponse(
                        installedAppsSignalDataWrapper = InstalledAppsSignalDataWrapper("."),
                        screenCaptureSignalDataWrapper = ScreenCaptureSignalDataWrapper("."),
                        screenOverlaySignalDataWrapper = ScreenOverlaySignalDataWrapper("."),
                        accessibilityAbuseSignalDataWrapper = AccessibilityAbuseSignalDataWrapper(),
                        displayListenerMetadataWrapper = DisplayListenerMetadataWrapper(
                            lastDisplayAddedTimeDelta = makeTimestamp(SystemClock.elapsedRealtimeNanos())
                        )
                    )
                )
                Log.d(TAG, "requestIntegrityToken integrityRequest: $integrityRequest")
                val integrityResponse = requestIntegritySyncData(context, authToken, integrityRequest)
                Log.d(TAG, "requestIntegrityToken integrityResponse: $integrityResponse")

                val integrityToken = integrityResponse.contentWrapper?.content?.token
                if (integrityToken.isNullOrEmpty()) {
                    if (integrityResponse.integrityResponseError?.error != null) {
                        throw StandardIntegrityException(IntegrityErrorCode.INTERNAL_ERROR, integrityResponse.integrityResponseError.error)
                    }
                    throw StandardIntegrityException(IntegrityErrorCode.INTERNAL_ERROR, "No token in response.")
                }

                Log.d(TAG, "requestIntegrityToken integrityToken: $integrityToken")
                integrityData?.updateAppIntegrityContent(context, System.currentTimeMillis(), "Delivered encrypted integrity token.", true)
                callback.onSuccess(packageName, integrityToken)
            }.onFailure {
                Log.w(TAG, "requestIntegrityToken has exception: ", it)
                integrityData?.updateAppIntegrityContent(context, System.currentTimeMillis(), "Integrity check failed: ${it.message}")
                callback.onError(integrityData?.packageName, IntegrityErrorCode.INTERNAL_ERROR, it.message ?: "Exception")
            }
        }
    }
}

private fun IIntegrityServiceCallback.onError(packageName: String?, errorCode: Int, errorMsg: String) {
    if (asBinder()?.isBinderAlive == false) {
        Log.e(TAG, "IIntegrityServiceCallback onError Binder died")
        return
    }
    Log.d(TAG, "requestIntegrityToken() failed for $packageName error -> $errorMsg")
    try {
        onRequestIntegrityToken(bundleOf("error" to errorCode))
    } catch (e: Exception) {
        Log.e(TAG, "exception $packageName error -> $e")
    }
}

private fun IIntegrityServiceCallback.onSuccess(packageName: String?, token: String) {
    if (asBinder()?.isBinderAlive == false) {
        Log.e(TAG, "IIntegrityServiceCallback onSuccess Binder died")
        return
    }
    Log.d(TAG, "requestIntegrityToken() success for $packageName)")
    try {
        onRequestIntegrityToken(bundleOf("token" to token))
    } catch (e: Exception) {
        Log.e(TAG, "exception $packageName error -> $e")
    }
}