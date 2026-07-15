/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.folsom.ui

import android.app.Activity
import android.util.Base64
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebResourceErrorCompat
import androidx.webkit.WebViewClientCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.microg.gms.auth.AuthManager
import org.microg.gms.auth.folsom.SECURITY_WEB_BASE_URL
import org.microg.gms.auth.folsom.buildKeyDeliveryInfo
import org.microg.gms.common.Constants.GMS_PACKAGE_NAME
import org.microg.gms.profile.Build.VERSION.SDK_INT
import java.net.URLEncoder
import java.util.Locale

private const val TAG = "FolsomWebViewHelper"

class FolsomWebViewHelper(
    private val fragment: Fragment,
    private val webView: WebView,
    private val accountName: String
) {
    fun prepareWebViewSettings() {
        webView.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = false
            databaseEnabled = false
            setNeedInitialFocus(false)
            useWideViewPort = false
            setSupportZoom(false)
            javaScriptCanOpenWindowsAutomatically = false
        }
    }

    fun setupWebViewClient(
        onPageStarted: (() -> Unit)? = null,
        onPageFinished: (() -> Unit)? = null
    ) {
        webView.webViewClient = object : WebViewClientCompat() {
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceErrorCompat) {
                Log.w(TAG, "Error loading: ${error.description}")
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                onPageStarted?.invoke()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                onPageFinished?.invoke()
            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                Log.d(TAG, "Navigating to $url")
                return url.toUri().host?.endsWith("google.com") != true
            }
        }
    }

    fun loadUrlWithAuthentication(targetUrl: String) {
        fragment.lifecycleScope.launch {
            val authUrl = withContext(Dispatchers.IO) {
                getAuthenticatedUrl(targetUrl)
            }
            if (authUrl != null) {
                setupCookies()
                webView.loadUrl(authUrl)
            } else {
                Log.w(TAG, "Failed to get authenticated URL")
                fragment.requireActivity().setResult(Activity.RESULT_CANCELED)
                fragment.requireActivity().finish()
            }
        }
    }

    private fun getAuthenticatedUrl(targetUrl: String): String? = runCatching {
        val service = "weblogin:continue=" + URLEncoder.encode(targetUrl, "UTF-8")
        AuthManager(fragment.requireContext(), accountName, GMS_PACKAGE_NAME, service)
            .requestAuthWithForegroundResolution(false)
            .auth
            ?.takeUnless { it.contains("WILL_NOT_SIGN_IN") }
    }.getOrNull()

    private fun setupCookies() {
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            if (SDK_INT >= 21) {
                setAcceptThirdPartyCookies(webView, true)
            }
        }
    }

    fun buildKeyRetrievalUrl(
        sessionId: String,
        securityDomain: String,
        offerReset: Boolean,
        darkMode: Boolean
    ): String {
        val locale = if (SDK_INT >= 21) {
            Locale.getDefault().toLanguageTag()
        } else {
            Locale.getDefault().language
        }
        val kdi = buildKeyDeliveryInfo(sessionId, securityDomain, offerReset)
        return SECURITY_WEB_BASE_URL.toUri().buildUpon().apply {
            appendQueryParameter("kdi", Base64.encodeToString(kdi, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
            if (locale.isNotEmpty()) appendQueryParameter("hl", locale)
            if (darkMode) appendQueryParameter("color_scheme", "dark")
        }.build().toString()
    }

    fun destroy() {
        webView.stopLoading()
    }
}