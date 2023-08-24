/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.appinivite

import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.LocaleList
import android.util.Log
import android.view.ViewGroup
import android.view.Window
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.os.bundleOf
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import com.android.volley.*
import com.android.volley.toolbox.Volley
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import kotlinx.coroutines.CompletableDeferred
import okio.ByteString.Companion.decodeHex
import org.microg.gms.appinvite.*
import org.microg.gms.common.Constants
import java.util.*

private const val TAG = "AppInviteActivity"

private const val APPINVITE_DEEP_LINK = "com.google.android.gms.appinvite.DEEP_LINK"
private const val APPINVITE_INVITATION_ID = "com.google.android.gms.appinvite.INVITATION_ID"
private const val APPINVITE_OPENED_FROM_PLAY_STORE = "com.google.android.gms.appinvite.OPENED_FROM_PLAY_STORE"
private const val APPINVITE_REFERRAL_BUNDLE = "com.google.android.gms.appinvite.REFERRAL_BUNDLE"

class AppInviteActivity : AppCompatActivity() {
    private val queue by lazy { Volley.newRequestQueue(this) }

    private val Int.px: Int get() = (this * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(ProgressBar(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(20.px)
            isIndeterminate = true
        })
        val extras = intent.extras
        extras?.keySet()
        Log.d(TAG, "Intent: $intent $extras")
        if (intent?.data == null) return finish()
        lifecycleScope.launchWhenStarted { run() }
    }

    private fun redirectToBrowser() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                data = intent.data
            })
        } catch (e: Exception) {
            Log.w(TAG, e)
        }
        finish()
    }

    private fun open(appInviteLink: MutateAppInviteLinkResponse) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            data = appInviteLink.data_?.intentData?.let { Uri.parse(it) }
            `package` = appInviteLink.data_?.packageName
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(
                APPINVITE_REFERRAL_BUNDLE, bundleOf(
                    APPINVITE_DEEP_LINK to appInviteLink,
                    APPINVITE_INVITATION_ID to "",
                    APPINVITE_OPENED_FROM_PLAY_STORE to false
                )
            )
        }
        val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            data = appInviteLink.data_?.fallbackUrl?.let { Uri.parse(it) }
        }
        val installedVersionCode = runCatching {
            intent.resolveActivity(packageManager)?.let {
                PackageInfoCompat.getLongVersionCode(packageManager.getPackageInfo(it.packageName, 0))
            }
        }.getOrNull()
        if (installedVersionCode != null && (appInviteLink.data_?.app?.minAppVersion == null || installedVersionCode >= appInviteLink.data_.app.minAppVersion)) {
            startActivity(intent)
            finish()
        } else {
            try {
                startActivity(fallbackIntent)
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
            finish()
        }
    }

    private suspend fun run() {
        val request = ProtobufPostRequest(
            "https://datamixer-pa.googleapis.com/v1/mutateonekey?alt=proto&key=AIzaSyAP-gfH3qvi6vgHZbSYwQ_XHqV_mXHhzIk", MutateOperation(
                id = MutateOperationId.AppInviteLink,
                mutateRequest = MutateDataRequest(
                    appInviteLink = MutateAppInviteLinkRequest(
                        client = ClientIdInfo(
                            packageName = Constants.GMS_PACKAGE_NAME,
                            signature = Constants.GMS_PACKAGE_SIGNATURE_SHA1.decodeHex().base64(),
                            language = Locale.getDefault().language
                        ),
                        link = LinkInfo(
                            empty = "",
                            uri = intent.data.toString()
                        ),
                        system = SystemInfo(
                            gms = SystemInfo.GmsInfo(
                                versionCode = Constants.GMS_VERSION_CODE
                            )
                        )
                    )
                )
            ), MutateDataResponseWithError.ADAPTER
        )
        val response = try {
            request.sendAndAwait(queue)
        } catch (e: Exception) {
            Log.w(TAG, e)
            return redirectToBrowser()
        }
        if (response.errorStatus != null || response.dataResponse?.appInviteLink == null) return redirectToBrowser()
        open(response.dataResponse.appInviteLink)
    }
}

class ProtobufPostRequest<I : Message<I, *>, O>(url: String, private val i: I, private val oAdapter: ProtoAdapter<O>) :
    Request<O>(Method.POST, url, null) {
    private val deferred = CompletableDeferred<O>()

    override fun getHeaders(): Map<String, String> {
        val headers = HashMap(super.getHeaders())
        headers["Accept-Language"] = if (SDK_INT >= 24) LocaleList.getDefault().toLanguageTags() else Locale.getDefault().language
        headers["X-Android-Package"] = Constants.GMS_PACKAGE_NAME
        headers["X-Android-Cert"] = Constants.GMS_PACKAGE_SIGNATURE_SHA1
        return headers
    }

    override fun getBody(): ByteArray = i.encode()

    override fun getBodyContentType(): String = "application/x-protobuf"

    override fun parseNetworkResponse(response: NetworkResponse): Response<O> {
        try {
            return Response.success(oAdapter.decode(response.data), null)
        } catch (e: VolleyError) {
            return Response.error(e)
        } catch (e: Exception) {
            return Response.error(VolleyError())
        }
    }

    override fun deliverResponse(response: O) {
        Log.d(TAG, "Got response: $response")
        deferred.complete(response)
    }

    override fun deliverError(error: VolleyError) {
        deferred.completeExceptionally(error)
    }

    suspend fun await(): O = deferred.await()

    suspend fun sendAndAwait(queue: RequestQueue): O {
        Log.d(TAG, "Sending request: $i")
        queue.add(this)
        return await()
    }
}