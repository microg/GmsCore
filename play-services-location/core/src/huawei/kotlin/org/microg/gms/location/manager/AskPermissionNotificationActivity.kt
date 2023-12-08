/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.manager

import android.Manifest
import android.Manifest.permission.*
import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
import androidx.core.content.getSystemService
import org.microg.gms.location.core.R
import org.microg.gms.location.core.BuildConfig
import org.microg.gms.utils.getApplicationLabel

@RequiresApi(23)
class AskPermissionNotificationActivity : AppCompatActivity() {

    private val foregroundRequestCode = 5
    private val backgroundRequestCode = 55
    private val sharedPreferences by lazy {
        getSharedPreferences(SHARED_PREFERENCE_NAME, MODE_PRIVATE)
    }
    private lateinit var hintView: View

    private lateinit var rationaleTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.extended_permission_request)
        rationaleTextView = findViewById(R.id.rationale_textview)

        if (checkAllPermissions()) {
            hideLocationPermissionNotification(this)
            finish()
        } else if (isGranted(ACCESS_COARSE_LOCATION) && isGranted(ACCESS_FINE_LOCATION) && !isGranted(ACCESS_BACKGROUND_LOCATION) && SDK_INT >= 29) {
            requestBackground()
        } else {
            requestForeground()
        }


        findViewById<View>(R.id.open_setting_tv).setOnClickListener {
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivityForResult(intent, 123)
        }

        findViewById<View>(R.id.decline_remind_tv).setOnClickListener {
            val editor = sharedPreferences.edit()
            editor.putBoolean(PERMISSION_REJECT_SHOW, true)
            editor.apply()
            finish()
        }

        hintView = findViewById(R.id.hint_sl)

        val hintTitle = getString(R.string.permission_hint_title)
        val builder = SpannableStringBuilder(hintTitle + getString(R.string.permission_hint))
        val span = ForegroundColorSpan(Color.BLACK)
        builder.setSpan(span, 0, hintTitle.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        builder.setSpan(StyleSpan(Typeface.BOLD), 0, hintTitle.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)

        val hintContentTv = findViewById<TextView>(R.id.hint_content_tv)
        hintContentTv.text = builder

        val showTimes = sharedPreferences.getInt(PERMISSION_SHOW_TIMES, 0)
        Log.d(TAG, "reject show times:$showTimes")
        if (showTimes >= 1) {
            hintView.visibility = View.VISIBLE
        }
    }

    private fun checkAllPermissions(): Boolean {
        if (SDK_INT < 23) return true
        return if (SDK_INT >= 29) {
            isGranted(ACCESS_COARSE_LOCATION)
                    && isGranted(ACCESS_FINE_LOCATION)
                    && isGranted(ACCESS_BACKGROUND_LOCATION)
        } else {
            isGranted(ACCESS_COARSE_LOCATION)
                    && isGranted(ACCESS_FINE_LOCATION)
        }
    }

    private fun isGranted(permission: String): Boolean {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkAndAddPermission(list: ArrayList<String>, permission: String) {
        val result = checkSelfPermission(permission)
        Log.i(TAG, "$permission: $result")
        if (result != PackageManager.PERMISSION_GRANTED) {
            list.add(permission)
        }
    }

    private fun requestForeground() {
        val appName = packageManager.getApplicationLabel(packageName)
        rationaleTextView.text = getString(R.string.rationale_foreground_permission, appName)
        val permissions = arrayListOf<String>()

        if (BuildConfig.FORCE_SHOW_BACKGROUND_PERMISSION.isNotEmpty()) {
            permissions.add(BuildConfig.FORCE_SHOW_BACKGROUND_PERMISSION)
        }
        checkAndAddPermission(permissions, ACCESS_COARSE_LOCATION)
        checkAndAddPermission(permissions, ACCESS_FINE_LOCATION)
        if (SDK_INT == 29) {
            rationaleTextView.text = getString(R.string.rationale_permission, appName)
            checkAndAddPermission(permissions, ACCESS_BACKGROUND_LOCATION)
        }
        requestPermissions(permissions, foregroundRequestCode)
    }

    private fun requestBackground() {
        rationaleTextView.setText(R.string.rationale_background_permission)
        val permissions = arrayListOf<String>()
        if (BuildConfig.FORCE_SHOW_BACKGROUND_PERMISSION.isNotEmpty()) {
            permissions.add(BuildConfig.FORCE_SHOW_BACKGROUND_PERMISSION)
        }
        if (SDK_INT >= 29) {
            checkAndAddPermission(permissions, ACCESS_BACKGROUND_LOCATION)
        }
        requestPermissions(permissions, backgroundRequestCode)
    }

    private fun requestPermissions(permissions: ArrayList<String>, requestCode: Int) {
        if (permissions.isNotEmpty()) {
            Log.w(TAG, "Request permissions: $permissions")
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), requestCode)
        } else {
            Log.i(TAG, "All permission granted")
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: ")
        checkPermissions()
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION)
        if (SDK_INT >= 29) permissions.add(ACCESS_BACKGROUND_LOCATION)

        if (permissions.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) {
            Log.d(TAG, "location permission is all granted")
            hideLocationPermissionNotification(this)
            finish()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            foregroundRequestCode -> {
                for (i in permissions.indices) {
                    val p = permissions[i]
                    val grant = grantResults[i]
                    val msg = if (grant == PackageManager.PERMISSION_GRANTED) "GRANTED" else "DENIED"
                    Log.w(TAG, "$p: $grant - $msg")
                }
                requestBackground()
            }

            backgroundRequestCode -> {
                if (isGranted(ACCESS_BACKGROUND_LOCATION)) {
                    hideLocationPermissionNotification(this)
                    setResult(RESULT_OK)
                    finish()
                } else {
                    reject()
                }
            }

            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
                reject()
            }
        }
    }

    private fun reject() {
        val showTimes = sharedPreferences.getInt(PERMISSION_SHOW_TIMES, 0)
        Log.d(TAG, "reject show times:$showTimes")
        if (showTimes >= 1) {
            hintView.visibility = View.VISIBLE
        } else {
            val editor = sharedPreferences.edit()
            editor.putInt(PERMISSION_SHOW_TIMES, showTimes + 1)
            editor.apply()
            setResult(RESULT_CANCELED)
            finish()
        }

    }


    companion object {
        private const val SHARED_PREFERENCE_NAME = "location_perm_notify"
        const val PERMISSION_SHOW_TIMES = "permission_show_times"
        const val PERMISSION_REJECT_SHOW = "permission_reject_show"
        private const val NOTIFICATION_ID = 1026359765

        private var notificationIsShown = false

        @JvmStatic
        fun showLocationPermissionNotification(context: Context) {
            if (notificationIsShown) return
            AskPermissionNotificationCancel.register(context)
            val appName = context.packageManager.getApplicationLabel(context.packageName).toString()
            val title = context.getString(R.string.location_permission_notification_title, appName)
            val backgroundPermissionOption =
                if (SDK_INT >= 30) context.packageManager.backgroundPermissionOptionLabel else context.getString(R.string.location_permission_background_option_name)
            val text = context.getString(R.string.location_permission_notification_content, backgroundPermissionOption, appName)
            val notification = NotificationCompat.Builder(context, createNotificationChannel(context))
                .setContentTitle(title).setContentText(text)
                .setSmallIcon(R.drawable.ic_permission_notification)
                .setContentIntent(PendingIntent.getActivity(context, 0, Intent(context, AskPermissionNotificationActivity::class.java), FLAG_IMMUTABLE))
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setDeleteIntent(PendingIntent.getBroadcast(context, 0, AskPermissionNotificationCancel.getTrigger(context), FLAG_IMMUTABLE))
                .build()
            context.getSystemService<NotificationManager>()?.notify(NOTIFICATION_ID, notification)
            notificationIsShown = true
        }

        @JvmStatic
        fun hideLocationPermissionNotification(context: Context) {
            if (!notificationIsShown) return
            context.getSystemService<NotificationManager>()?.cancel(NOTIFICATION_ID)
            AskPermissionNotificationCancel.unregister(context)
            notificationIsShown = false
        }

        @TargetApi(26)
        private fun createNotificationChannel(context: Context): String {
            val channelId = "missing-location-permission"
            if (SDK_INT >= 26) {
                val channel = NotificationChannel(channelId, "Missing location permission", NotificationManager.IMPORTANCE_HIGH)
                channel.setSound(null, null)
                channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                channel.setShowBadge(true)
                if (SDK_INT >= 29) {
                    channel.setAllowBubbles(false)
                }
                channel.vibrationPattern = longArrayOf(0)
                context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
                return channel.id
            }
            return channelId
        }

    }
}

@RequiresApi(23)
private object AskPermissionNotificationCancel : BroadcastReceiver() {
    private const val ACTION = "org.microg.gms.location.manager.ASK_PERMISSION_CANCEL"
    override fun onReceive(context: Context, intent: Intent?) {
        AskPermissionNotificationActivity.hideLocationPermissionNotification(context)
    }

    fun getTrigger(context: Context): Intent {
        return Intent(ACTION).apply { `package` = context.packageName }
    }

    fun register(context: Context) {
        ContextCompat.registerReceiver(context, this, IntentFilter(ACTION), RECEIVER_NOT_EXPORTED)
    }

    fun unregister(context: Context) {
        context.unregisterReceiver(this)
    }
}