/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.billing.ui

import android.accounts.Account
import android.accounts.AccountManager
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebResourceErrorCompat
import androidx.webkit.WebViewClientCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.microg.gms.profile.Build
import org.microg.vending.billing.TAG
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

class WebViewHelper(
    private val activity: ComponentActivity,
    private val webView: WebView,
    private val allowedPrefixes: Set<String> = emptySet()
) {
    fun openWebView(url: String, account: Account?) {
        prepareWebViewSettings(webView.settings)
        webView.webViewClient = object : WebViewClientCompat() {
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceErrorCompat) {
                Log.e(TAG, "Error loading: $error")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "onPageFinished $url")
                webView.visibility = View.VISIBLE
            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Navigating to $url")
                // TODO: figure out to not embed page of third parties, but allow bank confirmations
//                if (allowedPrefixes.isNotEmpty() && allowedPrefixes.none { url.startsWith(it) }) {
//                    try {
//                        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addCategory(Intent.CATEGORY_BROWSABLE) })
//                    } catch (e: Exception) {
//                        Log.d(TAG, "Error forwarding to browser", e)
//                    }
//                    return true
//                }
                return false
            }
        }

        val urlWithLanguage: String = addLanguageParam(url)
        if (account != null) {
            activity.lifecycleScope.launch(Dispatchers.IO) {
                openWebWithAccount(account, urlWithLanguage)
            }
        } else {
            loadWebViewUrl(urlWithLanguage)
        }
    }

    private fun loadWebViewUrl(url: String) {
        webView.loadUrl(url)
    }

    private fun addLanguageParam(url: String): String {
        val language = Locale.getDefault().language
        return if (language.isNotEmpty()) {
            Uri.parse(url).buildUpon().appendQueryParameter("hl", language).toString()
        } else {
            url
        }
    }

    private fun openWebWithAccount(account: Account, url: String) {
        try {
            val service = "weblogin:continue=" + URLEncoder.encode(url, "utf-8")
            val accountManager: AccountManager = AccountManager.get(activity)
            val future = accountManager.getAuthToken(account, service, null, null, null, null)
            val bundle = future.getResult(20, TimeUnit.SECONDS)
            val authUrl = bundle.getString(AccountManager.KEY_AUTHTOKEN)
                ?: throw RuntimeException("authUrl is null")
            if (authUrl.contains("WILL_NOT_SIGN_IN")) {
                throw RuntimeException("Would not sign in")
            }
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Opening $authUrl")
            webView.post {
                if (SDK_INT >= 21) {
                    CookieManager.getInstance().removeAllCookies {
                        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Cookies removed")
                        loadWebViewUrl(authUrl)
                    }
                } else {
                    CookieManager.getInstance().removeAllCookie()
                    loadWebViewUrl(authUrl)
                }

            }
        } catch (e: Exception) {
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Failed to get weblogin auth.", e)
            activity.finish()
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
        settings.userAgentString = Build.generateWebViewUserAgentString(settings.userAgentString)
    }
}