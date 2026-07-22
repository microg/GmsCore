/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.firebase.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.IBinder
import android.os.ResultReceiver
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
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG = "GmsFirebaseAuthCaptcha"

class ReCaptchaOverlayService : Service() {

    private var receiver: ResultReceiver? = null
    private var hostname: String? = null
    private var apiKey: String? = null

    private var finished = false
    private var container: View? = null
    private var windowManager: WindowManager? = null

    override fun onBind(intent: Intent): IBinder? {
        init(intent)
        return null
    }

    override fun onUnbind(intent: Intent?): Boolean {
        finishResult(Activity.RESULT_CANCELED)
        return super.onUnbind(intent)
    }

    private fun init(intent: Intent) {
        apiKey = intent.getStringExtra(EXTRA_API_KEY) ?: return finishResult(Activity.RESULT_CANCELED)
        receiver = intent.getParcelableExtra(EXTRA_RESULT_RECEIVER)
        hostname = intent.getStringExtra(EXTRA_HOSTNAME) ?: "localhost:5000"
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        show()
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
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

        val interceptorLayout: FrameLayout = object : FrameLayout(this) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    if (event.keyCode == KeyEvent.KEYCODE_BACK || event.keyCode == KeyEvent.KEYCODE_HOME) {
                        finishResult(Activity.RESULT_CANCELED)
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
                finishResult(Activity.RESULT_CANCELED)
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
            val captcha = assets.open("recaptcha.html").bufferedReader().readText().replace("%apikey%", apiKey!!)
            view.loadDataWithBaseURL("https://$hostname/", captcha, null, null, "https://$hostname/")
            windowManager?.addView(container, params)
        }
    }

    fun finishResult(resultCode: Int, token: String? = null) {
        if (!finished) {
            finished = true
            receiver?.send(resultCode, token?.let { Bundle().apply { putString(EXTRA_TOKEN, it) } })
        }
        container?.let { windowManager?.removeView(it) }
    }

    companion object {

        private val recaptchaServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                Log.d(TAG, "onReCaptchaToken: onServiceConnected: $name")
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.d(TAG, "onReCaptchaToken: onServiceDisconnected: $name")
            }
        }

        class ReCaptchaCallback(private val overlay: ReCaptchaOverlayService) {
            @JavascriptInterface
            fun onReCaptchaToken(token: String) {
                Log.d(TAG, "onReCaptchaToken: $token")
                overlay.finishResult(Activity.RESULT_OK, token)
            }
        }

        fun isSupported(context: Context): Boolean = android.os.Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(context)

        suspend fun awaitToken(context: Context, apiKey: String, hostname: String? = null) = suspendCoroutine { continuation ->
            val intent = Intent(context, ReCaptchaOverlayService::class.java)
            val resultReceiver = object : ResultReceiver(null) {
                override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                    context.unbindService(recaptchaServiceConnection)
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
            context.bindService(intent, recaptchaServiceConnection, BIND_AUTO_CREATE)
        }
    }
}
