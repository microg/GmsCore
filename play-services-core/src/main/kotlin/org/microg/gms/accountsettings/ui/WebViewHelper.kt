/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.accountsettings.ui

import android.content.Intent
import android.content.Intent.URI_INTENT_SCHEME
import android.net.Uri
import android.util.Log
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebResourceErrorCompat
import androidx.webkit.WebViewClientCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.microg.gms.auth.AuthManager
import org.microg.gms.common.Constants.GMS_PACKAGE_NAME
import org.microg.gms.common.PackageUtils
import java.net.URLEncoder
import java.util.*

private const val TAG = "AccountSettingsWebView"

class WebViewHelper(private val activity: AppCompatActivity, private val webView: WebView, private val allowedPrefixes: Set<String> = emptySet<String>()) {
    fun openWebView(url: String?, accountName: String?) {
        prepareWebViewSettings(webView.settings)
        webView.webViewClient = object : WebViewClientCompat() {
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceErrorCompat) {
                Log.w(TAG, "Error loading: $error")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webView.visibility = View.VISIBLE
            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                Log.d(TAG, "Navigating to $url")
                if (url.startsWith("intent:")) {
                    try {
                        val intent = Intent.parseUri(url, URI_INTENT_SCHEME)
                        if (intent.`package` == GMS_PACKAGE_NAME || PackageUtils.isGooglePackage(activity, intent.`package`)) {
                            // Only allow to start Google packages
                            activity.startActivity(intent)
                        } else {
                            Log.w(TAG, "Ignoring request to start non-Google app")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error invoking intent", e)
                    }
                    return false
                }
                if (allowedPrefixes.isNotEmpty() && allowedPrefixes.none { url.startsWith(it) }) {
                    try {
                        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addCategory(Intent.CATEGORY_BROWSABLE) })
                    } catch (e: Exception) {
                        Log.w(TAG, "Error forwarding to browser", e)
                    }
                    return true
                }
                return false
            }
        }

        val urlWithLanguage: String? = addLanguageParam(url)
        if (accountName != null) {
            activity.lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    openWebWithAccount(accountName, urlWithLanguage)
                }
            }
        } else {
            loadWebViewUrl(urlWithLanguage)
        }
    }

    private fun loadWebViewUrl(url: String?) {
        if (url != null) {
            webView.loadUrl(url)
        } else {
            activity.finish()
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

    private fun openWebWithAccount(accountName: String, url: String?) {
        try {
            val service = "weblogin:continue=" + URLEncoder.encode(url, "utf-8")
            val authManager = AuthManager(activity, accountName, GMS_PACKAGE_NAME, service)
            val authUrl = authManager.requestAuth(false)?.auth
            if (authUrl?.contains("WILL_NOT_SIGN_IN") == true) {
                throw RuntimeException("Would not sign in")
            }
            Log.d(TAG, "Opening $authUrl")
            webView.post {
                loadWebViewUrl(authUrl)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get weblogin auth.", e)
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
    }
}