/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.asterism.core

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

/**
 * Android Service hosting the Asterism API implementation.
 * Runs in the com.google.android.gms.unstable process to match
 * Google Play Services behavior for RCS-related services.
 */
class AsterismService : Service() {

    companion object {
        private const val TAG = "AsterismService"
    }

    private lateinit var apiService: AsterismApiService

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AsterismService created")
        apiService = AsterismApiService(this)
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "AsterismService bound, action=${intent?.action}")
        return apiService
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "AsterismService unbound")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        Log.d(TAG, "AsterismService destroyed")
        super.onDestroy()
    }
}
