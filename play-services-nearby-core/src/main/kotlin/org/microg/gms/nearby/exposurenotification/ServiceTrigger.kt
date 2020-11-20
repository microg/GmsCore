/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.microg.gms.common.ForegroundServiceContext

class ServiceTrigger : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "ServiceTrigger: $intent")
        val serviceContext = ForegroundServiceContext(context)
        if (ScannerService.isNeeded(context)) {
            Log.d(TAG, "Trigger ${ScannerService::class.java}")
            serviceContext.startService(Intent(context, ScannerService::class.java))
        }
        if (AdvertiserService.isNeeded(context)) {
            Log.d(TAG, "Trigger ${AdvertiserService::class.java}")
            serviceContext.startService(Intent(context, AdvertiserService::class.java))
        }
        if (CleanupService.isNeeded(context)) {
            Log.d(TAG, "Trigger ${CleanupService::class.java}")
            serviceContext.startService(Intent(context, CleanupService::class.java))
        }
    }
}
