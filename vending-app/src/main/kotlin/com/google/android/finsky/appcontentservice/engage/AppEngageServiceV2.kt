/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.finsky.appcontentservice.engage

import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService

class AppEngageServiceV2 : LifecycleService() {
    private val TAG = "AppEngageServiceV2"
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: ")
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        Log.d(TAG, "onBind: ")
        return AppEngageServiceV2Impl(lifecycle).asBinder()
    }

    override fun unbindService(conn: ServiceConnection) {
        super.unbindService(conn)
    }
}