/*
 * SPDX-FileCopyrightText: 2021 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.safetynet

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.net.http.SslCertificate
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.ResultReceiver
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.Window
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebViewClientCompat
import com.google.android.gms.safetynet.SafetyNetStatusCodes.*
import org.microg.gms.droidguard.core.DroidGuardResultCreator
import org.microg.gms.profile.Build
import org.microg.gms.profile.ProfileManager
import org.microg.gms.safetynet.core.ui.R
import java.io.ByteArrayInputStream
import java.net.URLEncoder
import java.security.MessageDigest
import kotlin.math.min

private const val TAG = "GmsReCAPTCHA"

private fun StringBuilder.appendUrlEncodedParam(key: String, value: String?) = append("&")
        .append(URLEncoder.encode(key, "UTF-8"))
        .append("=")
        .append(value?.let { URLEncoder.encode(it, "UTF-8") } ?: "")

class ReCaptchaActivity : AppCompatActivity() {
    private val receiver: ResultReceiver?
        get() = intent?.getParcelableExtra("result") as ResultReceiver?
    private val params: String?
        get() = intent?.getStringExtra("params")
    private val webView: WebView?
        get() = findViewById(R.id.recaptcha_webview)
    private val loading: View?
        get() = findViewById(R.id.recaptcha_loading)
    private val density: Float
        get() = resources.displayMetrics.density
    private val widthPixels: Int
        get() = resources.displayMetrics.widthPixels
    private val heightPixels: Int
        get() {
            val base = resources.displayMetrics.heightPixels
            val statusBarHeightId = resources.getIdentifier("status_bar_height", "dimen", "android")
            val statusBarHeight = if (statusBarHeightId > 0) resources.getDimensionPixelSize(statusBarHeightId) else 0
            return base - statusBarHeight - (density * 20.0).toInt()
        }
    private var resultSent: Boolean = false

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (receiver == null || params == null) {
            finish()
            return
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.recaptcha_window)
        webView?.apply {
            webViewClient = object : WebViewClientCompat() {
                fun String.isRecaptchaUrl() = startsWith("https://www.gstatic.com/recaptcha/") || startsWith("https://www.google.com/recaptcha/") || startsWith("https://www.google.com/js/bg/")

                override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
                    if (url.isRecaptchaUrl()) {
                        return null
                    }
                    return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(byteArrayOf()))
                }

                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    if (url.startsWith("https://support.google.com/recaptcha")) {
                        startActivity(Intent("android.intent.action.VIEW", Uri.parse(url)))
                        finish()
                        return true
                    }
                    return !url.isRecaptchaUrl()
                }
            }
            settings.apply {
                javaScriptEnabled = true
                useWideViewPort = true
                displayZoomControls = false
                setSupportZoom(false)
                cacheMode = WebSettings.LOAD_NO_CACHE
                ProfileManager.ensureInitialized(this@ReCaptchaActivity)
                userAgentString = Build.generateWebViewUserAgentString(userAgentString)
            }
            addJavascriptInterface(object {
                @JavascriptInterface
                fun challengeReady() {
                    Log.d(TAG, "challengeReady()")
                    runOnUiThread { webView?.loadUrl("javascript: RecaptchaMFrame.show(${min(widthPixels / density, 400f)}, ${min(heightPixels / density, 400f)});") }
                }

                @JavascriptInterface
                fun getClientAPIVersion() = 1

                @JavascriptInterface
                fun onChallengeExpired() {
                    Log.d(TAG, "onChallengeExpired()")
                }

                @JavascriptInterface
                fun onError(errorCode: Int, finish: Boolean) {
                    Log.d(TAG, "onError($errorCode, $finish)")
                    when (errorCode) {
                        1 -> sendErrorResult(ERROR, "Invalid Input Argument")
                        2 -> sendErrorResult(TIMEOUT, "Session Timeout")
                        7 -> sendErrorResult(RECAPTCHA_INVALID_SITEKEY, "Invalid Site Key")
                        8 -> sendErrorResult(RECAPTCHA_INVALID_KEYTYPE, "Invalid Type of Site Key")
                        9 -> sendErrorResult(RECAPTCHA_INVALID_PACKAGE_NAME, "Invalid Package Name for App")
                        else -> sendErrorResult(ERROR, "error")
                    }
                    if (finish) this@ReCaptchaActivity.finish()
                }

                @JavascriptInterface
                fun onResize(width: Int, height: Int) {
                    Log.d(TAG, "onResize($width, $height)")
                    if (webView?.visibility == View.VISIBLE) {
                        runOnUiThread { setWebViewSize(width, height, true) }
                    } else {
                        runOnUiThread { webView?.loadUrl("javascript: RecaptchaMFrame.shown($width, $height, true);") }
                    }
                }

                @JavascriptInterface
                fun onShow(visible: Boolean, width: Int, height: Int) {
                    Log.d(TAG, "onShow($visible, $width, $height)")
                    if (width <= 0 && height <= 0) {
                        runOnUiThread { webView?.loadUrl("javascript: RecaptchaMFrame.shown($width, $height, $visible);") }
                    } else {
                        runOnUiThread {
                            setWebViewSize(width, height, visible)
                            loading?.visibility = if (visible) View.GONE else View.VISIBLE
                            webView?.visibility = if (visible) View.VISIBLE else View.GONE
                        }
                    }
                }

                @JavascriptInterface
                fun requestToken(s: String, b: Boolean) {
                    Log.d(TAG, "requestToken($s, $b)")
                    runOnUiThread {
                        val cert = webView?.certificate?.let { Base64.encodeToString(SslCertificate.saveState(it).getByteArray("x509-certificate"), Base64.URL_SAFE + Base64.NO_PADDING + Base64.NO_WRAP) }
                                ?: ""
                        val params = StringBuilder(params).appendUrlEncodedParam("c", s).appendUrlEncodedParam("sc", cert).appendUrlEncodedParam("mt", System.currentTimeMillis().toString()).toString()
                        val flow = "recaptcha-android-${if (b) "verify" else "reload"}"
                        lifecycleScope.launchWhenResumed {
                            updateToken(flow, params)
                        }
                    }
                }

                @JavascriptInterface
                fun verifyCallback(token: String) {
                    Log.d(TAG, "verifyCallback($token)")
                    sendResult(0) { putString("token", token) }
                    resultSent = true
                    finish()
                }
            }, "RecaptchaEmbedder")
        }
        lifecycleScope.launchWhenResumed {
            open()
        }
    }

    fun sendErrorResult(errorCode: Int, error: String) = sendResult(errorCode) { putString("error", error); putInt("errorCode", errorCode) }

    fun sendResult(resultCode: Int, v: Bundle.() -> Unit) {
        receiver?.send(resultCode, Bundle().also(v))
        resultSent = true
    }

    override fun finish() {
        lifecycleScope.launchWhenResumed {
            webView?.loadUrl("javascript: RecaptchaMFrame.shown(0, 0, false);")
        }
        if (!resultSent) {
            sendErrorResult(CANCELED, "Cancelled")
        }
        super.finish()
    }

    fun setWebViewSize(width: Int, height: Int, visible: Boolean) {
        webView?.apply {
            layoutParams.width = min(widthPixels, (width * density).toInt())
            layoutParams.height = min(heightPixels, (height * density).toInt())
            requestLayout()
            loadUrl("javascript: RecaptchaMFrame.shown(${(layoutParams.width / density).toInt()}, ${(layoutParams.height / density).toInt()}, $visible);")
        }
    }

    suspend fun updateToken(flow: String, params: String) {
        val map = mapOf("contentBinding" to Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(params.toByteArray()), Base64.NO_WRAP))
        val dg = try {
            Base64.encodeToString(DroidGuardResultCreator.getResult(this, flow, map), Base64.NO_WRAP + Base64.URL_SAFE + Base64.NO_PADDING)
        } catch (e: Exception) {
            Log.w(TAG, e)
            Base64.encodeToString("ERROR : IOException".toByteArray(), Base64.NO_WRAP + Base64.URL_SAFE + Base64.NO_PADDING)
        }
        if (SDK_INT >= 19) {
            webView?.evaluateJavascript("RecaptchaMFrame.token('${URLEncoder.encode(dg, "UTF-8")}', '$params');", null)
        } else {
            webView?.loadUrl("javascript: RecaptchaMFrame.token('${URLEncoder.encode(dg, "UTF-8")}', '$params');")
        }
    }

    suspend fun open() {
        val params = StringBuilder(params).appendUrlEncodedParam("mt", System.currentTimeMillis().toString()).toString()
        val map = mapOf("contentBinding" to Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(params.toByteArray()), Base64.NO_WRAP))
        val dg = try {
            Base64.encodeToString(DroidGuardResultCreator.getResult(this, "recaptcha-android-frame", map), Base64.NO_WRAP + Base64.URL_SAFE + Base64.NO_PADDING)
        } catch (e: Exception) {
            Log.w(TAG, e)
            Base64.encodeToString("ERROR : IOException".toByteArray(), Base64.NO_WRAP + Base64.URL_SAFE + Base64.NO_PADDING)
        }
        webView?.postUrl(MFRAME_URL, "mav=1&dg=${URLEncoder.encode(dg, "UTF-8")}&mp=${URLEncoder.encode(params, "UTF-8")}".toByteArray())
    }

    companion object {
        private const val MFRAME_URL = "https://www.google.com/recaptcha/api2/mframe"
    }
}
