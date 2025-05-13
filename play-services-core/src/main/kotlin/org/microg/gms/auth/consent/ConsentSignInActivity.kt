/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.consent

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Message
import android.os.Messenger
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.core.os.bundleOf
import com.google.android.gms.R
import org.microg.gms.profile.Build.generateWebViewUserAgentString
import org.microg.gms.profile.ProfileManager

private const val TAG = "ConsentSignInActivity"
const val CONSENT_KEY_COOKIE = "cookie-"
const val CONSENT_URL = "consentUrl"
const val CONSENT_MESSENGER = "messenger"
const val CONSENT_RESULT = "consent_result"

class ConsentSignInActivity : Activity() {

    private var webView: WebView? = null
    private var progressBar: ProgressBar? = null
    private var sendSuccessResult = false

    private val consentUrl: String?
        get() = runCatching {
            intent?.getStringExtra(CONSENT_URL)
        }.getOrNull()

    private val messenger: Messenger?
        get() = runCatching {
            intent?.getParcelableExtra<Messenger>(CONSENT_MESSENGER)
        }.getOrNull()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consent_sign_in)
        ProfileManager.ensureInitialized(this)
        progressBar = findViewById(R.id.progressBar)
        webView = findViewById<WebView>(R.id.consent_sign)

        if (consentUrl == null || messenger == null) {
            finish()
            return
        }

        initWebView()
        initCookieManager()
    }

    private fun initWebView() {
        webView?.settings?.apply {
            userAgentString = generateWebViewUserAgentString(userAgentString)
            javaScriptEnabled = true
            setSupportMultipleWindows(false)
            saveFormData = false
            allowFileAccess = false
            databaseEnabled = false
            setNeedInitialFocus(false)
            useWideViewPort = false
            setSupportZoom(false)
            javaScriptCanOpenWindowsAutomatically = false
        }
        webView?.addJavascriptInterface(OAuthConsentInterface(), "OAuthConsent")
        webView?.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar?.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                progressBar?.visibility = View.GONE
            }
        }
    }

    private fun initCookieManager() {
        val cookieManager = CookieManager.getInstance()
        if (SDK_INT >= 21) {
            cookieManager.removeAllCookies { _ ->
                setCookiesAndLoadUrl(consentUrl!!, cookieManager)
            }
        } else {
            cookieManager.removeAllCookie()
            setCookiesAndLoadUrl(consentUrl!!, cookieManager)
        }
    }

    private fun setCookiesAndLoadUrl(consentUrl: String, cookieManager: CookieManager) {
        val extras = intent.extras
        if (extras != null && extras.size() > 0) {
            for (i in 0 until extras.size()) {
                val cookie = extras.getString(CONSENT_KEY_COOKIE + i)
                if (cookie != null) {
                    cookieManager.setCookie(consentUrl, cookie)
                }
            }
            webView?.loadUrl(consentUrl)
        } else {
            finish()
        }
    }

    private fun sendReplay(result: String?) {
        try {
            Log.d(TAG, "sendReplay result -> $result")
            val obtain = Message.obtain()
            obtain.data = bundleOf(Pair(CONSENT_RESULT, result))
            messenger?.send(obtain)
            sendSuccessResult = true
        } catch (e: Exception) {
            Log.w(TAG, "sendReplay Exception -> ", e)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "ConsentSignInActivity onDestroy ")
        super.onDestroy()
        if (!sendSuccessResult) {
            sendReplay(null)
        }
    }

    private inner class OAuthConsentInterface {
        @JavascriptInterface
        fun cancel() {
            Log.d(TAG, "consent cancel: sendReplay ")
            finish()
        }

        @get:JavascriptInterface
        val moduleVersion: Unit
            get() {
                Log.d(TAG, "getModuleVersion: ")
            }

        @JavascriptInterface
        fun setConsentResult(result: String) {
            Log.d(TAG, "consent success: sendReplay  -> $result")
            if ("CAA" != result) {
                sendReplay(result)
            }
            finish()
        }

        @JavascriptInterface
        fun showView() {
            Log.d(TAG, "consent showView: ")
        }
    }

    override fun onStop() {
        super.onStop()
        if (SDK_INT >= 21) {
            CookieManager.getInstance().removeAllCookies(null)
        } else {
            CookieManager.getInstance().removeAllCookie()
        }
    }
}
