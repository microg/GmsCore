/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.microg.gms.common.ForegroundServiceContext

class CleanupService : LifecycleService() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ForegroundServiceContext.completeForegroundService(this, intent, TAG)
        super.onStartCommand(intent, flags, startId)
        if (isNeeded(this)) {
            lifecycleScope.launchWhenStarted {
                launch(Dispatchers.IO) {
                    ExposureDatabase.with(this@CleanupService) {
                        it.dailyCleanup()
                    }
                    ExposurePreferences(this@CleanupService).lastCleanup = System.currentTimeMillis()
                }
                stopSelf()
            }
        } else {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    companion object {
        fun isNeeded(context: Context): Boolean {
            return ExposurePreferences(context).let {
                it.enabled && it.lastCleanup < System.currentTimeMillis() - CLEANUP_INTERVAL
            }
        }
    }
}
