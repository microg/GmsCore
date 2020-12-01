/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.core.ui

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ResultReceiver
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.microg.gms.nearby.exposurenotification.*
import org.microg.gms.ui.getApplicationInfoIfExists

class ExposureNotificationsConfirmActivity : AppCompatActivity() {
    private var resultCode: Int = RESULT_CANCELED
        set(value) {
            setResult(value)
            field = value
        }
    private val receiver: ResultReceiver?
        get() = intent.getParcelableExtra(KEY_CONFIRM_RECEIVER)
    private val action: String?
        get() = intent.getStringExtra(KEY_CONFIRM_ACTION)
    private val targetPackageName: String?
        get() = intent.getStringExtra(KEY_CONFIRM_PACKAGE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.exposure_notifications_confirm_activity)
        val applicationInfo = packageManager.getApplicationInfoIfExists(targetPackageName)
        val selfApplicationInfo = packageManager.getApplicationInfoIfExists(packageName)
        when (action) {
            CONFIRM_ACTION_START -> {
                findViewById<TextView>(android.R.id.title).text = getString(R.string.exposure_confirm_start_title)
                findViewById<TextView>(android.R.id.summary).text = getString(R.string.exposure_confirm_start_summary, applicationInfo?.loadLabel(packageManager)
                        ?: targetPackageName)
                findViewById<Button>(android.R.id.button1).text = getString(R.string.exposure_confirm_start_button)
                findViewById<TextView>(R.id.grant_permission_summary).text = getString(R.string.exposure_confirm_permission_description, selfApplicationInfo?.loadLabel(packageManager)
                        ?: packageName)
                checkPermissions()
            }
            CONFIRM_ACTION_STOP -> {
                findViewById<TextView>(android.R.id.title).text = getString(R.string.exposure_confirm_stop_title)
                findViewById<TextView>(android.R.id.summary).text = getString(R.string.exposure_confirm_stop_summary)
                findViewById<Button>(android.R.id.button1).text = getString(R.string.exposure_confirm_stop_button)
            }
            CONFIRM_ACTION_KEYS -> {
                findViewById<TextView>(android.R.id.title).text = getString(R.string.exposure_confirm_keys_title, applicationInfo?.loadLabel(packageManager)
                        ?: targetPackageName)
                findViewById<TextView>(android.R.id.summary).text = getString(R.string.exposure_confirm_keys_summary)
                findViewById<Button>(android.R.id.button1).text = getString(R.string.exposure_confirm_keys_button)
            }
            else -> {
                resultCode = RESULT_CANCELED
                finish()
            }
        }
        findViewById<Button>(android.R.id.button1).setOnClickListener {
            resultCode = RESULT_OK
            finish()
        }
        findViewById<Button>(android.R.id.button2).setOnClickListener {
            resultCode = RESULT_CANCELED
            finish()
        }
        findViewById<Button>(R.id.grant_permission_button).setOnClickListener {
            requestPermissions()
        }
    }

    private val permissions by lazy {
        if (Build.VERSION.SDK_INT >= 29) {
            arrayOf("android.permission.ACCESS_BACKGROUND_LOCATION", "android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION")
        } else {
            arrayOf("android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION")
        }
    }
    private var requestCode = 33
    private fun checkPermissions() {
        val needRequest = Build.VERSION.SDK_INT >= 23 && permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        findViewById<Button>(android.R.id.button1).isEnabled = !needRequest
        findViewById<View>(R.id.grant_permission_view).visibility = if (needRequest) View.VISIBLE else View.GONE
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(permissions, ++requestCode)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == this.requestCode) checkPermissions()
    }

    override fun finish() {
        receiver?.send(resultCode, Bundle())
        super.finish()
    }
}
