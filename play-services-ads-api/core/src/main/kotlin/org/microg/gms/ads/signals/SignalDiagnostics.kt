/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.ads.signals

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "AdsSignalGenerator"

/** Limit repeated JS diagnostics. Callers supply only fixed event/reason names, never payloads. */
internal object SignalDiagnostics {
    private val counts = ConcurrentHashMap<String, AtomicInteger>()

    fun debug(event: String, reason: String) {
        val key = "$event:$reason"
        val count = counts.getOrPut(key) { AtomicInteger() }.incrementAndGet()
        if (count <= 5) Log.d(TAG, "$event: reason=$reason count=$count")
        else if (count == 6) Log.d(TAG, "$event: reason=$reason further_logs_suppressed")
    }
}
