/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.service

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.edit

private const val TAG = "FitnessPermission"
private const val PREFS_NAME = "fitness-permissions"
private const val PERMISSION_REQUESTED = "activity-recognition-requested"
private const val REQUEST_ACTIVITY_RECOGNITION = 1

internal fun Context.ensureActivityRecognitionPermission(): Boolean {
    if (android.os.Build.VERSION.SDK_INT < 29 ||
        checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
    ) return true

    val preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    if (!preferences.getBoolean(PERMISSION_REQUESTED, false)) {
        runCatching {
            startActivity(Intent(this, FitnessPermissionActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            })
        }.onFailure { Log.w(TAG, "Unable to request activity recognition permission", it) }
    }
    return false
}

internal class FitnessPermissionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT < 29 ||
            checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        ) {
            finish()
            return
        }
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit { putBoolean(PERMISSION_REQUESTED, true) }
        requestPermissions(arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), REQUEST_ACTIVITY_RECOGNITION)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_ACTIVITY_RECOGNITION) finish()
    }
}
