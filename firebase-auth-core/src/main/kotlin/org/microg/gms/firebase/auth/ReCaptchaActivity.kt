/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.firebase.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.os.Bundle
import android.os.ResultReceiver
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import org.microg.gms.firebase.auth.core.R
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG = "GmsFirebaseAuthCaptcha"

class ReCaptchaActivity : AppCompatActivity() {
    private val receiver: ResultReceiver?
        get() = intent.getParcelableExtra(EXTRA_RESULT_RECEIVER)
    private val hostname: String
        get() = intent.getStringExtra(EXTRA_HOSTNAME) ?: "localhost:5000"
    private var finished = false

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openWebsite()
    }

    private fun openWebsite() {
        val apiKey = intent.getStringExtra(EXTRA_API_KEY) ?: return finishResult(Activity.RESULT_CANCELED)
        setContentView(R.layout.activity_recaptcha)
        val view = findViewById<WebView>(R.id.web)
        val settings = view.settings
        settings.javaScriptEnabled = true
        settings.useWideViewPort = false
        settings.setSupportZoom(false)
        settings.displayZoomControls = false
        settings.cacheMode = WebSettings.LOAD_NO_CACHE
        view.addJavascriptInterface(object : Any() {
            @JavascriptInterface
            fun onReCaptchaToken(token: String) {
                Log.d(TAG, "onReCaptchaToken: $token")
                finishResult(Activity.RESULT_OK, token)
            }
        }, "MyCallback")
        val captcha = assets.open("recaptcha.html").bufferedReader().readText().replace("%apikey%", apiKey)
        view.loadDataWithBaseURL("https://$hostname/", captcha, null, null, "https://$hostname/")
    }

    fun finishResult(resultCode: Int, token: String? = null) {
        finished = true
        setResult(resultCode, token?.let { Intent().apply { putExtra(EXTRA_TOKEN, it) } })
        receiver?.send(resultCode, token?.let { Bundle().apply { putString(EXTRA_TOKEN, it) } })
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!finished) receiver?.send(Activity.RESULT_CANCELED, null)
    }

    companion object {
        const val EXTRA_TOKEN = "token"
        const val EXTRA_API_KEY = "api_key"
        const val EXTRA_HOSTNAME = "hostname"
        const val EXTRA_RESULT_RECEIVER = "receiver"

        fun isSupported(context: Context): Boolean = true

        suspend fun awaitToken(context: Context, apiKey: String, hostname: String? = null) = suspendCoroutine<String> { continuation ->
            val intent = Intent(context, ReCaptchaActivity::class.java)
            val resultReceiver = object : ResultReceiver(null) {
                override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                    try {
                        if (resultCode == Activity.RESULT_OK) {
                            continuation.resume(resultData?.getString(EXTRA_TOKEN)!!)
                        }
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }
            }
            intent.putExtra(EXTRA_API_KEY, apiKey)
            intent.putExtra(EXTRA_RESULT_RECEIVER, resultReceiver)
            intent.putExtra(EXTRA_HOSTNAME, hostname)
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(FLAG_ACTIVITY_REORDER_TO_FRONT)
            intent.addFlags(FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            context.startActivity(intent)
        }
    }
}
