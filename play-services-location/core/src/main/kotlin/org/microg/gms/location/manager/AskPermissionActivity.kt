/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.manager

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Message
import android.os.Messenger
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import org.microg.gms.location.core.BuildConfig

const val EXTRA_MESSENGER = "messenger"
const val EXTRA_PERMISSIONS = "permissions"
const val EXTRA_GRANT_RESULTS = "results"

private const val REQUEST_CODE_PERMISSION = 120

class AskPermissionActivity : AppCompatActivity() {
    private var permissionGrants = IntArray(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "AskPermissionActivity: onCreate")
        requestPermissions()
    }

    private fun requestPermissions(permissions: Array<String> = intent?.getStringArrayExtra(EXTRA_PERMISSIONS) ?: emptyArray()) {
        val realPermissions = permissions.toMutableList().apply {
            if (BuildConfig.FORCE_SHOW_BACKGROUND_PERMISSION.isNotEmpty()) add(BuildConfig.FORCE_SHOW_BACKGROUND_PERMISSION)
        }.toTypedArray()
        permissionGrants = permissions.map { ContextCompat.checkSelfPermission(this, it) }.toIntArray()
        ActivityCompat.requestPermissions(this, realPermissions, REQUEST_CODE_PERMISSION)
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
            val forceShowBackground = permissions.contains(BuildConfig.FORCE_SHOW_BACKGROUND_PERMISSION)
            val realGrantResults = if (forceShowBackground) grantResults.copyOfRange(0, grantResults.size - 1) else grantResults
            Log.d(TAG, "onRequestPermissionsResult: permissions:${permissions.size} forceShowBackground:$forceShowBackground")
            Log.d(TAG, "onRequestPermissionsResult: grantResults:${grantResults.size} realGrantResults:${realGrantResults.size}")
            if (SDK_INT >= 30) {
                val backgroundRequested = permissions.contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                val backgroundDenied = backgroundRequested && realGrantResults[permissions.indexOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)] == PackageManager.PERMISSION_DENIED
                val onlyBackgroundDenied = backgroundDenied && realGrantResults.count { it == PackageManager.PERMISSION_DENIED } == 1
                val someAccepted = !permissionGrants.contentEquals(realGrantResults)
                if (onlyBackgroundDenied && someAccepted) {
                    // Only background denied, ask again as some systems require that
                    requestPermissions()
                    return
                }
            }
            if (BuildConfig.SHOW_NOTIFICATION_WHEN_NOT_PERMITTED) {
                val clazz = runCatching { Class.forName("org.microg.gms.location.manager.AskPermissionNotificationActivity") }.getOrNull()
                if (realGrantResults.any { it == PackageManager.PERMISSION_DENIED } ){
                    runCatching { clazz?.getDeclaredMethod("showLocationPermissionNotification", Context::class.java)?.invoke(null, this@AskPermissionActivity) }
                } else {
                    runCatching { clazz?.getDeclaredMethod("hideLocationPermissionNotification", Context::class.java)?.invoke(null, this@AskPermissionActivity) }
                }
            }
            sendReply(extras = bundleOf(EXTRA_GRANT_RESULTS to realGrantResults))
            finish()
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}