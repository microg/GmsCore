/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.SystemClock
import android.os.WorkSource
import org.microg.gms.location.EXTRA_LOCATION
import org.microg.gms.location.EXTRA_ELAPSED_REALTIME

class NetworkLocationRequest(
    var pendingIntent: PendingIntent,
    var intervalMillis: Long,
    var lowPower: Boolean,
    var bypass: Boolean,
    var workSource: WorkSource
) {
    var lastRealtime = 0L
        private set

    fun send(context: Context, location: Location) {
        lastRealtime = SystemClock.elapsedRealtime()
        pendingIntent.send(context, 0, Intent().apply {
            putExtra(EXTRA_LOCATION, location)
            putExtra(EXTRA_ELAPSED_REALTIME, lastRealtime)
        })
    }
}