/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.billing.ui

import android.accounts.Account
import android.accounts.AccountManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Message
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebResourceErrorCompat
import androidx.webkit.WebViewClientCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
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
    fun openWebView(url: String, account: Account?, webAction: WebViewAction) {
        prepareWebViewSettings(webView.settings, webAction == WebViewAction.ADD_PAYMENT_METHOD)
        webView.webChromeClient = PayWebChromeClient(webAction)
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

    private fun prepareWebViewSettings(settings: WebSettings, enableMultiWindow: Boolean = false) {
        settings.javaScriptEnabled = true
        settings.setSupportMultipleWindows(enableMultiWindow)
        settings.allowFileAccess = false
        settings.databaseEnabled = false
        settings.setNeedInitialFocus(false)
        settings.useWideViewPort = false
        settings.setSupportZoom(false)
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.userAgentString = Build.generateWebViewUserAgentString(settings.userAgentString)
    }

    private class PayWebChromeClient(val webAction: WebViewAction) : WebChromeClient() {

        override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
            Log.d(TAG, "onCreateWindow: isDialog:$isDialog isUserGesture:$isUserGesture resultMsg: ${resultMsg.toString()}")
            if (webAction != WebViewAction.ADD_PAYMENT_METHOD) {
                return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
            }
            // Add third-party payment methods, such as PayPal/GrabPay/Alipay.
            // WebView needs to support multi-window mode, and the child view will call the js method registered by the parent view
            // to refresh the page and close it by itself.
            var bottomSheetDialog: BottomSheetDialog? = null
            val parentContext = view!!.context
            val subWebView = WebView(parentContext).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                }
                webViewClient = object : WebViewClientCompat() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        Log.d(TAG, "sub_window starts loading: $url")
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d(TAG, "sub_window loaded : $url")
                        visibility = View.VISIBLE
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onCloseWindow(window: WebView?) {
                        Log.d(TAG, "sub_window closed")
                        bottomSheetDialog?.dismiss()
                    }
                }
                layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
                visibility = View.INVISIBLE
            }
            bottomSheetDialog = BottomSheetDialog(parentContext).apply {
                setOnDismissListener { subWebView.destroy() }
            }
            val layout = RelativeLayout(parentContext)
            layout.addView(ProgressBar(parentContext).apply {
                layoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    addRule(RelativeLayout.CENTER_HORIZONTAL)
                    addRule(RelativeLayout.CENTER_VERTICAL)
                }
                isIndeterminate = true
            })
            bottomSheetDialog.setContentView(layout.apply { addView(subWebView) })
            bottomSheetDialog.show()

            (resultMsg?.obj as? WebView.WebViewTransport)?.webView = subWebView
            resultMsg?.sendToTarget()
            return true
        }
    }
}