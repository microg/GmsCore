/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.phone

import android.Manifest
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Message
import android.os.Messenger
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf

private const val TAG = "AskPermission"
private const val REQUEST_CODE_PERMISSION = 101
private val ALLOWED_PERMISSIONS = setOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_CONTACTS)

class AskPermissionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissions = intent.getStringArrayExtra(EXTRA_PERMISSIONS) ?: arrayOf(Manifest.permission.RECEIVE_SMS)
        Log.d(TAG, "Requesting permissions: ${permissions.toList()}")
        if (SDK_INT < 23 || permissions.any { it !in ALLOWED_PERMISSIONS }) {
            sendReply(RESULT_CANCELED)
            finish()
        } else {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSION)
        }
    }

    private fun sendReply(code: Int = RESULT_OK, extras: Bundle = Bundle.EMPTY) {
        intent.getParcelableExtra<Messenger>(EXTRA_MESSENGER)?.let {
            it.send(Message.obtain().apply {
                what = code
                data = extras
            })
        }
        setResult(code, Intent().apply { putExtras(extras) })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSION) {
            sendReply(extras = bundleOf(EXTRA_GRANT_RESULTS to grantResults))
            finish()
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}