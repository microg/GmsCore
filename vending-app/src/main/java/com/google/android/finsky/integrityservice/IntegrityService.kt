/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.integrityservice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.util.Base64
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.finsky.ResponseWrapper
import com.google.android.gms.droidguard.DroidGuard
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest
import com.google.android.gms.tasks.await
import com.google.android.play.core.integrity.model.IntegrityErrorCode
import com.google.android.play.core.integrity.protocol.IIntegrityService
import com.google.android.play.core.integrity.protocol.IIntegrityServiceCallback
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.request.accept
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.userAgent
import kotlinx.coroutines.launch
import okio.ByteString.Companion.toByteString
import org.microg.vending.FINSKY_USER_AGENT
import org.microg.vending.utils.SIGNING_FLAGS
import org.microg.vending.utils.encodeBase64
import org.microg.vending.utils.getPackageInfoCompat
import org.microg.vending.utils.sha256
import org.microg.vending.utils.signaturesCompat
import java.io.InputStream
import java.time.Instant

private const val TAG = "IntegrityService"

class IntegrityService : LifecycleService() {
    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return IntegrityServiceImpl(this, lifecycle).asBinder()
    }
}

private const val DROIDGUARD_FLOW = "pia_attest_e1"

class IntegrityServiceImpl(
    private val context: Context,
    private val lifecycle: Lifecycle,
) : IIntegrityService.Stub(), LifecycleOwner {
    override fun getLifecycle(): Lifecycle = lifecycle

    // TODO use OkHttp or CIO
    private val httpClient = HttpClient(Android)

    override fun requestIntegrityToken(request: Bundle, callback: IIntegrityServiceCallback) {
        val callingUid = getCallingUid()

        lifecycleScope.launch {
            try {
                val packageName = request.getString("package.name")
                val nonce = request.getByteArray("nonce")
                val cloudProjectNumber = request.getLongOrNull("cloud.prj")
                val playCoreVersion = PlayCoreVersion(
                    request.getInt("playcore.integrity.version.major", 1),
                    request.getInt("playcore.integrity.version.minor", 0),
                    request.getInt("playcore.integrity.version.patch", 0),
                )

                Log.d(
                    TAG,
                    "requestIntegrityToken(packageName: $packageName, nonce: ${nonce?.encodeBase64(false)}, cloudProjectNumber: $cloudProjectNumber, playCoreVersion: $playCoreVersion)"
                )

                if (packageName == null) throw IntegrityException(IntegrityErrorCode.INTERNAL_ERROR, "Package name missing")

                if (nonce == null) throw IntegrityException(IntegrityErrorCode.INTERNAL_ERROR, "Nonce missing")
                if (nonce.count() < 16) throw IntegrityException(IntegrityErrorCode.NONCE_TOO_SHORT)
                if (nonce.count() > 500) throw IntegrityException(IntegrityErrorCode.NONCE_TOO_LONG)

                val packageInfo = context.packageManager.getPackageInfoCompat(packageName, SIGNING_FLAGS)
                if (packageInfo.applicationInfo.uid != callingUid) {
                    throw IntegrityException(
                        IntegrityErrorCode.APP_UID_MISMATCH,
                        "UID for the requested package name (${packageInfo.applicationInfo.uid}) doesn't match the calling UID ($callingUid)"
                    )
                }

                val certificateSha256Digests = packageInfo.signaturesCompat.map { it.toByteArray().sha256().encodeBase64(true) }

                val versionCode = packageInfo.versionCode

                val timestamp = Instant.now()

                val details = IntegrityRequest.Details(
                    packageName = IntegrityRequest.Details.PackageNameWrapper(packageName),
                    versionCode = IntegrityRequest.Details.VersionCodeWrapper(versionCode),
                    nonce = nonce.encodeBase64(false),
                    certificateSha256Digests = certificateSha256Digests,
                    timestampAtRequest = timestamp,
                    cloudProjectNumber = cloudProjectNumber
                )

                val data = mutableMapOf(
                    "pkg_key" to packageName,
                    "vc_key" to versionCode.toString(),
                    "nonce_sha256_key" to nonce.sha256().encodeBase64(true),
                    "tm_s_key" to timestamp.epochSecond.toString(),
                    "binding_key" to details.encode().encodeBase64(false),
                )

                if (cloudProjectNumber != null) {
                    data["gcp_n_key"] = cloudProjectNumber.toString()
                }

                val droidGuardResultsRequest = DroidGuardResultsRequest()
                droidGuardResultsRequest.bundle.putString("thirdPartyCallerAppPackageName", packageName)

                Log.d(TAG, "Running DroidGuard (flow: $DROIDGUARD_FLOW, data: $data)")

                val droidGuardToken = DroidGuard.getClient(context).getResults(DROIDGUARD_FLOW, data, droidGuardResultsRequest).await()

                val droidGuardTokenRaw = Base64.decode(droidGuardToken, Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE).toByteString()

                // TODO change how errors work in microg droidguard?
                if (droidGuardTokenRaw.utf8().startsWith("ERROR :")) {
                    throw IntegrityException(IntegrityErrorCode.INTERNAL_ERROR, "DroidGuard failed")
                }

                val integrityRequest = IntegrityRequest(
                    details = details,
                    flowName = DROIDGUARD_FLOW,
                    droidGuardTokenRaw = droidGuardTokenRaw,
                    playCoreVersion = playCoreVersion,
                    playProtectDetails = PlayProtectDetails(PlayProtectState.PLAY_PROTECT_STATE_NO_PROBLEMS),
                )

                Log.d(TAG, "Calling Integrity API (integrityRequest: $integrityRequest)")
                val response = httpClient.post("https://play-fe.googleapis.com/fdfe/integrity") {
                    setBody(integrityRequest.encode())
                    headers {
                        Log.d(TAG, "userAgent: $FINSKY_USER_AGENT")
                        userAgent(FINSKY_USER_AGENT)

                        ContentType("application", "x-protobuf").let {
                            contentType(it)
                            accept(it)
                        }

                        // TODO this should be enough because integrity doesn't require auth, but maybe should we do the whole X-PS-RH dance anyway?
                        append("X-DFE-Device-Id", "1")
                    }
                }

                val responseWrapper = ResponseWrapper.ADAPTER.decode(response.body<InputStream>())
                Log.d(TAG, "Integrity API response: $responseWrapper")

                val integrityResponse = responseWrapper.payload?.integrityResponse
                if (integrityResponse?.token == null) {
                    throw IntegrityException(
                        when (response.status.value) {
                            429 -> IntegrityErrorCode.TOO_MANY_REQUESTS
                            460 -> IntegrityErrorCode.CLIENT_TRANSIENT_ERROR
                            else -> IntegrityErrorCode.NETWORK_ERROR
                        }, "IntegrityResponse didn't have a token"
                    )
                }

                callback.onRequestIntegrityToken(integrityResponse.token)
            } catch (e: IntegrityException) {
                Log.e(TAG, "requestIntegrityToken failed", e)
                callback.onRequestIntegrityToken(e.errorCode)
            }
        }
    }

    class IntegrityException(@IntegrityErrorCode val errorCode: Int, message: String? = null) : Exception(message)
}

private fun Bundle.getLongOrNull(key: String): Long? {
    return if (containsKey(key)) getLong(key) else null
}
