/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.consent

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Message
import android.os.Messenger
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.core.os.bundleOf
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.google.android.gms.R
import org.microg.gms.common.Constants.GMS_PACKAGE_NAME
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

    private val isDarkTheme: Boolean
        get() = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

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
        initLayout()
        initWebView()
        initCookieManager()
    }

    private fun initLayout() {
        val layoutParams = window.attributes as WindowManager.LayoutParams
        layoutParams.width = (resources.displayMetrics.widthPixels * 0.8).toInt()
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        window.attributes = layoutParams
    }

    private fun initWebView() {
        webView?.settings?.apply {
            // Advertise dark theme to Google's OcId sign-in pages so they serve
            // the native dark variant instead of relying on algorithmic darkening.
            userAgentString = generateWebViewUserAgentString(userAgentString) +
                " OcIdWebView ({\"os\":\"Android\",\"osVersion\":$SDK_INT,\"app\":\"$GMS_PACKAGE_NAME\",\"callingAppId\":\"\",\"isDarkTheme\":$isDarkTheme})"
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
        webView?.applyDarkMode()
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        webView?.applyDarkMode()
    }

    @Suppress("DEPRECATION")
    private fun WebView.applyDarkMode() {
        if (SDK_INT < 33) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(
                    settings,
                    if (isDarkTheme) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
                )
            }
        } else if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, isDarkTheme)
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
