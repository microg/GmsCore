/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.feedback.ui

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewClientCompat
import org.microg.gms.common.Constants
import java.util.Locale

private const val TAG = "FeedbackAlohaActivity"

private val FEEDBACK_URL = hashMapOf(
    "com.google.android.apps.maps" to "https://support.google.com/maps?#topic=3093612",
    "com.android.chrome" to "https://support.google.com/chrome?#topic=7439538",
    "com.google.android.youtube" to "https://support.google.com/youtube?#topic=9257498",
    "com.google.android.gm" to "https://support.google.com/mail?#topic=7065107",
    "com.google.android.googlequicksearchbox" to "https://support.google.com/websearch?#topic=3378866",
    "com.google.android.apps.ads.publisher" to "https://support.google.com/adsense?#topic=3373519",
    "com.google.android.apps.cloudconsole" to "https://support.google.com/googlecloud",
    "com.google.android.apps.adwords" to "https://support.google.com/google-ads?#topic=10286612",
    "com.google.android.apps.photos" to "https://support.google.com/photos?#topic=6128818",
    "com.google.android.apps.kids.familylink" to "https://support.google.com/families?#topic=7327495",
    "com.google.android.apps.tycho" to "https://support.google.com/fi?#topic=4596407",
    "com.google.android.apps.nbu.paisa.user" to "https://support.google.com/googlepay?#topic=12369512",
    "com.google.android.apps.docs" to "https://support.google.com/drive?#topic=14940",
    "com.google.android.apps.shopping.express" to "https://support.google.com/googleshopping?#topic=9112782",
)

class FeedbackAlohaActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate")
        val callingPackage = intent?.getStringExtra(Constants.KEY_PACKAGE_NAME) ?: callingPackage ?: return finish()
        Log.d(TAG, "callingPackage: $callingPackage")
        val url = FEEDBACK_URL[callingPackage] ?: return finish()
        Log.d(TAG, "url: $url")

        val layout = RelativeLayout(this)
        layout.addView(ProgressBar(this).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.CENTER_HORIZONTAL)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
            isIndeterminate = true
        })
        webView = WebView(this).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.INVISIBLE
        }
        layout.addView(webView)
        setContentView(layout)

        prepareWebViewSettings(webView.settings)

        webView.webViewClient = object : WebViewClientCompat() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webView.visibility = View.VISIBLE
            }
        }
        Log.d(TAG, "Open $url for $callingPackage")
        loadWebViewUrl(addLanguageParam(url))
    }

    private fun loadWebViewUrl(url: String?) {
        if (url != null) {
            webView.loadUrl(url)
        } else {
            finish()
        }
    }

    private fun addLanguageParam(url: String?): String? {
        val language = Locale.getDefault().language
        return if (language.isNotEmpty()) {
            Uri.parse(url).buildUpon().appendQueryParameter("hl", language).toString()
        } else {
            url
        }
    }

    private fun prepareWebViewSettings(settings: WebSettings) {
        settings.javaScriptEnabled = true
        settings.setSupportMultipleWindows(false)
        settings.allowFileAccess = false
        settings.databaseEnabled = false
        settings.setNeedInitialFocus(false)
        settings.useWideViewPort = false
        settings.setSupportZoom(false)
        settings.javaScriptCanOpenWindowsAutomatically = false
    }

}
