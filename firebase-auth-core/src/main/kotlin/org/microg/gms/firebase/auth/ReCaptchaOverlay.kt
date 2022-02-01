/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.firebase.auth

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import org.microg.gms.firebase.auth.core.R
import org.microg.gms.profile.Build
import org.microg.gms.profile.ProfileManager
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


private const val TAG = "GmsFirebaseAuthCaptcha"

class ReCaptchaOverlay(val context: Context, val apiKey: String, val hostname: String?, val continuation: Continuation<String>) {

    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    var finished = false
    var container: View? = null

    private fun show() {
        val layoutParamsType = if (android.os.Build.VERSION.SDK_INT >= 26) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutParamsType,
                0,
                PixelFormat.TRANSLUCENT)

        params.gravity = Gravity.CENTER or Gravity.START
        params.x = 0
        params.y = 0

        val interceptorLayout: FrameLayout = object : FrameLayout(context) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    if (event.keyCode == KeyEvent.KEYCODE_BACK || event.keyCode == KeyEvent.KEYCODE_HOME) {
                        cancel()
                        return true
                    }
                }
                return super.dispatchKeyEvent(event)
            }
        }

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as? LayoutInflater?
        if (inflater != null) {
            val container = inflater.inflate(R.layout.activity_recaptcha, interceptorLayout)
            this.container = container
            container.setBackgroundResource(androidx.appcompat.R.drawable.abc_dialog_material_background)
            val pad = (5.0 * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)).toInt()
            container.setOnTouchListener { v, _ ->
                v.performClick()
                cancel()
                return@setOnTouchListener true
            }
            val view = container.findViewById<WebView>(R.id.web)
            view.setPadding(pad, pad, pad, pad)
            val settings = view.settings
            settings.javaScriptEnabled = true
            settings.useWideViewPort = false
            settings.setSupportZoom(false)
            settings.displayZoomControls = false
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            ProfileManager.ensureInitialized(context)
            settings.userAgentString = Build.generateWebViewUserAgentString(settings.userAgentString)
            view.addJavascriptInterface(object : Any() {
                @JavascriptInterface
                fun onReCaptchaToken(token: String) {
                    Log.d(TAG, "onReCaptchaToken: $token")
                    if (!finished) {
                        finished = true
                        continuation.resume(token)
                    }
                    close()
                }
            }, "MyCallback")
            val captcha = context.assets.open("recaptcha.html").bufferedReader().readText().replace("%apikey%", apiKey)
            view.loadDataWithBaseURL("https://$hostname/", captcha, null, null, "https://$hostname/")
            windowManager.addView(container, params)
        }
    }

    fun cancel() {
        if (!finished) {
            finished = true
            continuation.resumeWithException(RuntimeException("User cancelled"))
        }
        close()
    }

    fun close() {
        container?.let { windowManager.removeView(it) }
    }

    companion object {
        fun isSupported(context: Context): Boolean = android.os.Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(context)

        suspend fun awaitToken(context: Context, apiKey: String, hostname: String? = null) = suspendCoroutine<String> { continuation ->
            ReCaptchaOverlay(context, apiKey, hostname ?: "localhost:5000", continuation).show()
        }
    }
}
