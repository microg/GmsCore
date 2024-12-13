/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.firebase.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.ResultReceiver
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import org.microg.gms.firebase.auth.core.R
import org.microg.gms.profile.Build
import org.microg.gms.profile.ProfileManager
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG = "GmsFirebaseAuthCaptcha"

class ReCaptchaOverlayActivity : AppCompatActivity() {

    private val receiver: ResultReceiver?
        get() = intent.getParcelableExtra(EXTRA_RESULT_RECEIVER)
    private val hostname: String
        get() = intent.getStringExtra(EXTRA_HOSTNAME) ?: "localhost:5000"
    private var finished = false
    private var container: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        show()
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    private fun show() {
        val apiKey = intent.getStringExtra(EXTRA_API_KEY) ?: return cancel()
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
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

        val interceptorLayout: FrameLayout = object : FrameLayout(this) {
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

        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as? LayoutInflater?
        if (inflater != null) {
            val container = inflater.inflate(R.layout.activity_recaptcha, interceptorLayout)
            this.container = container
            container.setBackgroundResource(androidx.appcompat.R.drawable.abc_dialog_material_background)
            val pad = (5.0 * (resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)).toInt()
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
            ProfileManager.ensureInitialized(this)
            settings.userAgentString = Build.generateWebViewUserAgentString(settings.userAgentString)
            view.addJavascriptInterface(ReCaptchaCallback(this), "MyCallback")
            val captcha = assets.open("recaptcha.html").bufferedReader().readText().replace("%apikey%", apiKey)
            view.loadDataWithBaseURL("https://$hostname/", captcha, null, null, "https://$hostname/")
            windowManager.addView(container, params)
        }
    }

    fun cancel() {
        if (!finished) {
            finished = true
            finishResult(Activity.RESULT_CANCELED)
        }
        close()
    }

    fun close() {
        container?.let { windowManager.removeView(it) }
    }

    fun finishResult(resultCode: Int, token: String? = null) {
        finished = true
        setResult(resultCode, token?.let { Intent().apply { putExtra(EXTRA_TOKEN, it) } })
        receiver?.send(resultCode, token?.let { Bundle().apply { putString(EXTRA_TOKEN, it) } })
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    companion object {
        class ReCaptchaCallback(private val overlay: ReCaptchaOverlayActivity) {
            @JavascriptInterface
            fun onReCaptchaToken(token: String) {
                Log.d(TAG, "onReCaptchaToken: $token")
                if (!overlay.finished) {
                    overlay.finished = true
                    overlay.finishResult(Activity.RESULT_OK, token)
                }
                overlay.close()
            }
        }

        fun isSupported(context: Context): Boolean = android.os.Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(context)

        suspend fun awaitToken(context: Context, apiKey: String, hostname: String? = null) = suspendCoroutine { continuation ->
            val intent = Intent(context, ReCaptchaOverlayActivity::class.java)
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
