/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.installer

import android.os.Bundle
import android.os.Message
import android.os.Messenger
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.vending.R
import org.microg.gms.utils.getApplicationLabel
import org.microg.gms.vending.AllowType

class AskInstallReminderActivity : AppCompatActivity() {

    private lateinit var permissionDesc: TextView
    private lateinit var appIconView: ImageView
    private lateinit var appNameView: TextView
    private lateinit var checkBox: CheckBox
    private lateinit var btnAllow: Button
    private lateinit var btnClose: ImageView
    private var isNotShowAgainChecked: Boolean = false
    private var isBtnClick: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_install_reminder)
        setupViews()
        setupListeners()
    }

    private fun setupViews() {
        val callerPackage = intent.extras?.getString(EXTRA_CALLER_PACKAGE)
        val installPackage = intent.extras?.getString(EXTRA_INSTALL_PACKAGE)
        val appIcon = intent?.getByteArrayExtra(EXTRA_INSTALL_PACKAGE_ICON)?.toDrawableOrNull(this)
        val appName = intent?.getStringExtra(EXTRA_INSTALL_PACKAGE_NAME)

        permissionDesc = findViewById(R.id.tv_description)
        appIconView = findViewById(R.id.iv_app_icon)
        appIcon?.let { appIconView.setImageDrawable(it) }
        appNameView = findViewById(R.id.tv_app_name)
        appNameView.text = appName ?: installPackage
        checkBox = findViewById(R.id.cb_dont_show_again)
        checkBox.setOnCheckedChangeListener { _, isChecked -> isNotShowAgainChecked = isChecked }

        btnAllow = findViewById(R.id.btn_allow)
        btnClose = findViewById(R.id.btn_close)

        permissionDesc.apply {
            if (!callerPackage.isNullOrEmpty()) {
                val displayName = packageManager.getApplicationLabel(callerPackage)
                text = getString(R.string.channel_install_allow_to_install_third_app, displayName)
            } else {
                text = getString(R.string.channel_install_allow_to_install_third_app, "")
            }
        }
    }

    private fun setupListeners() {
        btnClose.setOnClickListener {
            isBtnClick = true
            finishWithReply(if (isNotShowAgainChecked) AllowType.ALLOWED_NEVER.value else AllowType.ALLOWED_REQUEST.value)
        }
        btnAllow.setOnClickListener {
            isBtnClick = true
            finishWithReply(if (isNotShowAgainChecked) AllowType.ALLOWED_ALWAYS.value else AllowType.ALLOWED_SINGLE.value)
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isBtnClick) {
            finishWithReply()
        }
    }

    private fun finishWithReply(code: Int = AllowType.ALLOWED_REQUEST.value) {
        intent?.getParcelableExtra<Messenger>(EXTRA_MESSENGER)?.let {
            runCatching {
                it.send(Message.obtain().apply { what = code })
            }
        }
        finish()
    }
}