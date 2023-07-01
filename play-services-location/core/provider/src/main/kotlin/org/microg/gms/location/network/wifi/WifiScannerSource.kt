/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network.wifi

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.wifi.ScanResult
import android.net.wifi.WifiScanner
import android.os.Build.VERSION.SDK_INT
import android.os.WorkSource
import android.util.Log
import androidx.core.content.ContextCompat

@SuppressLint("WrongConstant")
class WifiScannerSource(private val context: Context, private val callback: WifiDetailsCallback) : WifiDetailsSource {
    override fun startScan(workSource: WorkSource?) {
        val scanner = context.getSystemService("wifiscanner") as WifiScanner
        scanner.startScan(WifiScanner.ScanSettings(), object : WifiScanner.ScanListener {
            override fun onSuccess() {
                Log.d(org.microg.gms.location.network.TAG, "Not yet implemented: onSuccess")
            }

            override fun onFailure(reason: Int, description: String?) {
                Log.d(org.microg.gms.location.network.TAG, "Not yet implemented: onFailure ${reason} ${description}")
            }

            override fun onPeriodChanged(periodInMs: Int) {
                Log.d(org.microg.gms.location.network.TAG, "Not yet implemented: onPeriodChanged")
            }

            override fun onResults(results: Array<out WifiScanner.ScanData>) {
                callback.onWifiDetailsAvailable(results.flatMap { it.results.toList() }.map(ScanResult::toWifiDetails))
            }

            override fun onFullResult(fullScanResult: ScanResult) {
                Log.d(org.microg.gms.location.network.TAG, "Not yet implemented: onFullResult")
            }
        }, workSource)
    }

    companion object {
        fun isSupported(context: Context): Boolean {
            return SDK_INT >= 26 && (context.getSystemService("wifiscanner") as? WifiScanner) != null && ContextCompat.checkSelfPermission(context, Manifest.permission.LOCATION_HARDWARE) == PERMISSION_GRANTED
        }
    }
}