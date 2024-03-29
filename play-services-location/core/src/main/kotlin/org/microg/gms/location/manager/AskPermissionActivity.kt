/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.manager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Message
import android.os.Messenger
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf

const val EXTRA_MESSENGER = "messenger"
const val EXTRA_PERMISSIONS = "permissions"
const val EXTRA_GRANT_RESULTS = "results"

private const val REQUEST_CODE_PERMISSION = 120

class AskPermissionActivity : AppCompatActivity() {
    private var permissionGrants = IntArray(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions()
    }

    private fun requestPermissions(permissions: Array<String> = intent?.getStringArrayExtra(EXTRA_PERMISSIONS) ?: emptyArray()) {
        permissionGrants = permissions.map { ContextCompat.checkSelfPermission(this, it) }.toIntArray()
        ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSION)
    }

    private fun sendReply(code: Int = RESULT_OK, extras: Bundle = Bundle.EMPTY) {
        intent?.getParcelableExtra<Messenger>(EXTRA_MESSENGER)?.let {
            runCatching {
                it.send(Message.obtain().apply {
                    what = code
                    data = extras
                })
            }
        }
        setResult(code, Intent().apply { putExtras(extras) })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (SDK_INT >= 30) {
                val backgroundRequested = permissions.contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                val backgroundDenied = backgroundRequested && grantResults[permissions.indexOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)] == PackageManager.PERMISSION_DENIED
                val onlyBackgroundDenied = backgroundDenied && grantResults.count { it == PackageManager.PERMISSION_DENIED } == 1
                val someAccepted = !permissionGrants.contentEquals(grantResults)
                if (onlyBackgroundDenied && someAccepted) {
                    // Only background denied, ask again as some systems require that
                    requestPermissions()
                    return
                }
            }
            sendReply(extras = bundleOf(EXTRA_GRANT_RESULTS to grantResults))
            finish()
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}