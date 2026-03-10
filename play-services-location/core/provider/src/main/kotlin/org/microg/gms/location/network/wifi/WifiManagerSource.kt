/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network.wifi

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.WorkSource
import android.util.Log
import androidx.core.content.getSystemService

class WifiManagerSource(private val context: Context, private val callback: WifiDetailsCallback) : BroadcastReceiver(), WifiDetailsSource {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        try {
            callback.onWifiDetailsAvailable(this.context.getSystemService<WifiManager>()?.scanResults.orEmpty().map(ScanResult::toWifiDetails))
        } catch (e: Exception) {
            Log.w(org.microg.gms.location.network.TAG, e)
        }
    }

    override fun enable() {
        context.registerReceiver(this, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
    }

    override fun disable() {
        context.unregisterReceiver(this)
    }

    override fun startScan(workSource: WorkSource?) {
        context.getSystemService<WifiManager>()?.startScan()
    }
}