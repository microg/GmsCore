/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.googlehelp.ui

import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable.Creator
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.Volley
import com.google.android.gms.googlehelp.GoogleHelp
import com.google.android.gms.googlehelp.InProductHelp
import org.microg.gms.accountsettings.ui.WebViewHelper
import org.microg.gms.googlehelp.CallerAppInfo
import org.microg.gms.googlehelp.DeviceInfo
import org.microg.gms.googlehelp.RequestBody
import org.microg.gms.googlehelp.RequestContent
import org.microg.gms.googlehelp.ResponseContentWarp
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG = "GoogleHelpRedirect"
private const val GOOGLE_HELP_KEY = "EXTRA_GOOGLE_HELP"
private const val PRODUCT_HELP_KEY = "EXTRA_IN_PRODUCT_HELP"

private const val HELP_URL = "https://www.google.com/tools/feedback/mobile/help-suggestions"

class GoogleHelpRedirectActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate begin")
        if (intent == null) {
            Log.d(TAG, "onCreate intent is null")
            finish()
            return
        }
        val callingPackage = callingPackage ?: callingActivity?.packageName ?: return finish()
        Log.d(TAG, "onCreate callingPackage: $callingPackage")
        val googleHelp = intent.getParcelableExtra<GoogleHelp>(GOOGLE_HELP_KEY)
        var inProductHelp: InProductHelp? = null
        if (googleHelp == null) {
            inProductHelp = getParcelableFromIntent<InProductHelp>(intent, PRODUCT_HELP_KEY, InProductHelp.CREATOR)
        }
        Log.d(TAG, "onCreate: googleHelp: ${googleHelp ?: inProductHelp?.googleHelp}")

        val layout = RelativeLayout(this)
        layout.addView(ProgressBar(this).apply {
            layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                addRule(RelativeLayout.CENTER_HORIZONTAL)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
            isIndeterminate = true
        })
        webView = WebView(this).apply {
            layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
            visibility = View.INVISIBLE
        }
        layout.addView(webView)
        setContentView(layout)

        lifecycleScope.launchWhenCreated {
            val account = googleHelp?.account ?: inProductHelp?.googleHelp?.account
            val searchId = googleHelp?.appContext ?: inProductHelp?.googleHelp?.appContext
            Log.d(TAG, "loadHelpUrl: searchId: $searchId")
            val answerUrl = requestHelpLink(callingPackage, searchId).content?.info?.answerUrl
            Log.d(TAG, "answerUrl: $answerUrl")
            val url = googleHelp?.uri?.toString() ?: inProductHelp?.googleHelp?.uri?.toString() ?: answerUrl
            Log.d(TAG, "loadUrl: $url")
            if (answerUrl != null) {
                WebViewHelper(this@GoogleHelpRedirectActivity, webView).openWebView(url, account?.name)
                setResult(RESULT_OK)
            } else finish()
        }
    }

    override fun onBackPressed() {
        if (this::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private fun <T> getParcelableFromIntent(intent: Intent, key: String?, creator: Creator<T>): T? {
        try {
            val data = intent.getByteArrayExtra(key)
            if (data != null) {
                val parcel = Parcel.obtain()
                parcel.unmarshall(data, 0, data.size)
                parcel.setDataPosition(0)
                val result = creator.createFromParcel(parcel)
                parcel.recycle()
                return result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing InProductHelp", e)
        }
        return null
    }

    private suspend fun requestHelpLink(callingPackage: String, searchId: String?) = suspendCoroutine { sus ->
        Volley.newRequestQueue(this.applicationContext).add(object : Request<ResponseContentWarp>(Method.POST, HELP_URL, {
            Log.d(TAG, "requestHelpLink: ", it)
            sus.resumeWithException(it)
        }) {

            override fun deliverResponse(response: ResponseContentWarp) {
                Log.d(TAG, "requestHelpLink response: $response")
                sus.resume(response)
            }

            override fun getBody(): ByteArray {
                return RequestContent.Builder().apply {
                    appInfo = CallerAppInfo.Builder().apply { packageName = callingPackage }.build()
                    deviceInfo = DeviceInfo.Builder().apply { language = Locale.getDefault().language }.build()
                    body = RequestBody.Builder().apply { appContext = searchId }.build()
                }.build().also {
                    Log.d(TAG, "requestBody: $it")
                }.encode()
            }

            override fun getBodyContentType(): String = "application/x-protobuf"

            override fun parseNetworkResponse(response: NetworkResponse): Response<ResponseContentWarp> {
                return try {
                    Response.success(ResponseContentWarp.ADAPTER.decode(response.data), null)
                } catch (e: Exception) {
                    Response.error(VolleyError(e))
                }
            }
        })
    }
}