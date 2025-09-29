/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.locationsharingreporter.service.settings

import android.os.Bundle
import android.os.Message
import android.os.Messenger
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

const val EXTRA_MESSENGER = "messenger"
class LocationShareConfirmActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val messenger = intent.getParcelableExtra<Messenger>(EXTRA_MESSENGER)
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle(R.string.location_share_dialog_title)
            .setCancelable(false)
            .setMessage(R.string.location_share_dialog_msg)
            .setPositiveButton(android.R.string.ok) { _, _ ->

                messenger?.send(Message.obtain().apply { what = RESULT_OK })
                finish()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                messenger?.send(Message.obtain().apply { what = RESULT_CANCELED })
                finish()
            }
        builder.show()
    }
}