/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.microg.gms.common.ForegroundServiceContext

class ServiceTrigger : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent?) {
        if (ExposurePreferences(context).scannerEnabled) {
            ForegroundServiceContext(context).startService(Intent(context, ScannerService::class.java))
        }
        if (ExposurePreferences(context).advertiserEnabled) {
            ForegroundServiceContext(context).startService(Intent(context, AdvertiserService::class.java))
        }
    }
}
