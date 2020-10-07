/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.core.ui

import android.os.Bundle
import android.os.ResultReceiver
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.microg.gms.nearby.core.ui.R
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
        when (action) {
            CONFIRM_ACTION_START -> {
                findViewById<TextView>(android.R.id.title).text = getString(R.string.exposure_confirm_start_title)
                findViewById<TextView>(android.R.id.summary).text = getString(R.string.exposure_confirm_start_summary, applicationInfo?.loadLabel(packageManager)
                        ?: targetPackageName)
                findViewById<Button>(android.R.id.button1).text = getString(R.string.exposure_confirm_start_button)
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
    }

    override fun finish() {
        receiver?.send(resultCode, Bundle())
        super.finish()
    }
}
