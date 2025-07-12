/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.games.utils

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.gms.R
import com.google.android.gms.common.images.ImageManager
import java.lang.ref.WeakReference

class AccountPromptManager private constructor(context: Context) {

    private val appContextRef = WeakReference(context.applicationContext)
    private val handler = Handler(Looper.getMainLooper())

    private var windowManagerRef: WeakReference<WindowManager>? = null
    private var promptViewRef: WeakReference<View>? = null
    private val removeRunnable = Runnable { remove() }

    companion object {
        private const val TAG = "AccountPromptManager"

        @Volatile
        private var instance: AccountPromptManager? = null

        fun getInstance(context: Context): AccountPromptManager {
            return instance ?: synchronized(this) {
                instance ?: AccountPromptManager(context).also { instance = it }
            }
        }
    }

    fun show(username: String, avatarUrl: String) {
        Log.d(TAG, "show: username=$username, avatarUrl=$avatarUrl")
        if (promptViewRef?.get() != null) return
        val context = appContextRef.get() ?: return
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManagerRef = WeakReference(windowManager)

        val view = LayoutInflater.from(context).inflate(R.layout.item_view_game_account_prompt, null)
        promptViewRef = WeakReference(view)

        val promptRoot = view.findViewById<LinearLayout>(R.id.account_prompt_root)
        val logo = view.findViewById<ImageView>(R.id.game_logo)
        val avatar = view.findViewById<ImageView>(R.id.player_avatar)
        val usernameView = view.findViewById<TextView>(R.id.player_name)
        val infoContainer = view.findViewById<View>(R.id.account_info_container)

        usernameView.text = String.format(context.getString(R.string.games_popup_signin_welcome), username)

        ImageManager.create(context).loadImage(avatarUrl, avatar)

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 80
        }

        try {
            windowManager.addView(view, layoutParams)
        } catch (e: Exception) {
            Log.w(TAG, "windowManager addView failed ", e)
        }

        fun prepare() {
            Log.d(TAG, "prepare")
            promptRoot.visibility = View.VISIBLE
            infoContainer.visibility = View.GONE
        }

        fun showPlayer() {
            Log.d(TAG, "showPlayer")
            logo.visibility = View.GONE
            infoContainer.visibility = View.VISIBLE
            infoContainer.alpha = 0f
            infoContainer.animate().alpha(1f).setDuration(500).start()
        }

        fun hidePlayer() {
            Log.d(TAG, "hidePlayer")
            infoContainer.animate().setDuration(500).withEndAction {
                infoContainer.visibility = View.GONE
                logo.visibility = View.VISIBLE
            }.start()
        }

        fun hidePromptRoot() {
            Log.d(TAG, "hidePromptRoot")
            promptRoot.animate().scaleX(0f).scaleY(0f).setDuration(300).withEndAction {
                promptRoot.visibility = View.GONE
                handler.post(removeRunnable)
            }.start()
        }

        handler.postDelayed({ prepare() }, 1000)
        handler.postDelayed({ showPlayer() }, 1500)
        handler.postDelayed({ hidePlayer() }, 3500)
        handler.postDelayed({ hidePromptRoot() }, 4500)
    }

    fun remove() {
        Log.d(TAG, "remove")
        handler.removeCallbacksAndMessages(null)

        promptViewRef?.get()?.let { view ->
            try {
                windowManagerRef?.get()?.removeView(view)
            } catch (_: Exception) {
            }
        }

        promptViewRef?.clear()
        windowManagerRef?.clear()
        promptViewRef = null
        windowManagerRef = null
    }
}