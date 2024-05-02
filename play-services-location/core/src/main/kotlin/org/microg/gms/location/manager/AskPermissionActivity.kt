/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.manager

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Message
import android.os.Messenger
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import org.microg.gms.common.Constants
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
        if (firstRequestLocationSettingsDialog) {
            requestPermissions()
        } else {
            Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", packageName, null)
            }
        }
    }

    private fun requestPermissions(permissions: Array<String> = intent?.getStringArrayExtra(EXTRA_PERMISSIONS) ?: emptyArray()) {
        permissionGrants = permissions.map {
            if (BuildConfig.FORCE_SHOW_BACKGROUND_PERMISSION.isNotEmpty() && BuildConfig.FORCE_SHOW_BACKGROUND_PERMISSION == it) {
                PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(this, it)
            }
        }.toIntArray()
        if (permissionGrants.all { it == PackageManager.PERMISSION_GRANTED }) {
            sendReply(extras = bundleOf(EXTRA_GRANT_RESULTS to permissionGrants))
            finish()
        } else {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSION)
        }
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
            Log.d(TAG, "onRequestPermissionsResult: permissions:${permissions.joinToString(",")} grantResults:${grantResults.joinToString(",")}")
            if (SDK_INT >= 30) {
                val backgroundRequested = permissions.contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                if (BuildConfig.FORCE_SHOW_BACKGROUND_PERMISSION.isNotEmpty() && permissions.contains(BuildConfig.FORCE_SHOW_BACKGROUND_PERMISSION)) {
                    grantResults[permissions.indexOf(BuildConfig.FORCE_SHOW_BACKGROUND_PERMISSION)] =
                        PackageManager.PERMISSION_GRANTED
                }
                grantResults.forEach { Log.d(TAG, "onRequestPermissionsResult: $it") }
                permissionGrants.forEach { Log.d(TAG, "onRequestPermissionsResult permissionGrants: $it") }
                val backgroundDenied = backgroundRequested && grantResults[permissions.indexOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)] == PackageManager.PERMISSION_DENIED
                val onlyBackgroundDenied = backgroundDenied && grantResults.count { it == PackageManager.PERMISSION_DENIED } == 1
                val someAccepted = !permissionGrants.contentEquals(grantResults)
                Log.d(TAG, "onRequestPermissionsResult onlyBackgroundDenied: $onlyBackgroundDenied someAccepted:$someAccepted")
                if (onlyBackgroundDenied && someAccepted) {
                    // Only background denied, ask again as some systems require that
                    requestPermissions()
                    return
                }
            }
            firstRequestLocationSettingsDialog = false
            if (BuildConfig.SHOW_NOTIFICATION_WHEN_NOT_PERMITTED) {
                val clazz = runCatching { Class.forName("org.microg.gms.location.manager.AskPermissionNotificationActivity") }.getOrNull()
                if (grantResults.any { it == PackageManager.PERMISSION_DENIED } ){
                    runCatching { clazz?.getDeclaredMethod("showLocationPermissionNotification", Context::class.java)?.invoke(null, this@AskPermissionActivity.applicationContext) }
                } else {
                    runCatching { clazz?.getDeclaredMethod("hideLocationPermissionNotification", Context::class.java)?.invoke(null, this@AskPermissionActivity.applicationContext) }
                }
            }
            sendReply(extras = bundleOf(EXTRA_GRANT_RESULTS to grantResults))
            finish()
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    companion object {
        private var firstRequestLocationSettingsDialog: Boolean = true
    }
}