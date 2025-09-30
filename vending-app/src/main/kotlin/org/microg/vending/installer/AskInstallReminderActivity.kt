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
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.android.vending.R
import org.microg.gms.utils.getApplicationLabel
import org.microg.gms.vending.AllowType

@RequiresApi(21)
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
        val callerPackage = intent.extras?.getString(EXTRA_CALLER_PACKAGE)?.takeIf { it.isNotEmpty() }
            ?: return finishWithReply(AllowType.REJECT_ONCE.value)
        val callerLabel = runCatching { packageManager.getApplicationLabel(callerPackage) }.getOrNull()
            ?: return finishWithReply(AllowType.REJECT_ONCE.value)
        val appIcon = intent?.getByteArrayExtra(EXTRA_INSTALL_PACKAGE_ICON)?.toDrawableOrNull(this)
        val appLabel = intent?.getStringExtra(EXTRA_INSTALL_PACKAGE_LABEL)?.takeIf { it.isNotEmpty() }
            ?: return finishWithReply(AllowType.REJECT_ONCE.value)

        permissionDesc = findViewById(R.id.tv_description)
        permissionDesc.text = getString(R.string.channel_install_allow_to_install_third_app, callerLabel)
        appIconView = findViewById(R.id.iv_app_icon)
        appIcon?.let { appIconView.setImageDrawable(it) }
        appNameView = findViewById(R.id.tv_app_name)
        appNameView.text = appLabel
        checkBox = findViewById(R.id.cb_dont_show_again)
        checkBox.setOnCheckedChangeListener { _, isChecked -> isNotShowAgainChecked = isChecked }

        btnAllow = findViewById(R.id.btn_allow)
        btnClose = findViewById(R.id.btn_close)
    }

    private fun setupListeners() {
        btnClose.setOnClickListener {
            isBtnClick = true
            finishWithReply(if (isNotShowAgainChecked) AllowType.REJECT_ALWAYS.value else AllowType.REJECT_ONCE.value)
        }
        btnAllow.setOnClickListener {
            isBtnClick = true
            finishWithReply(if (isNotShowAgainChecked) AllowType.ALLOW_ALWAYS.value else AllowType.ALLOW_ONCE.value)
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isBtnClick) {
            finishWithReply()
        }
    }

    private fun finishWithReply(code: Int = AllowType.REJECT_ONCE.value) {
        intent?.getParcelableExtra<Messenger>(EXTRA_MESSENGER)?.let {
            runCatching {
                it.send(Message.obtain().apply { what = code })
            }
        }
        finishAndRemoveTask()
    }
}