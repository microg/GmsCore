/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout

@SuppressLint("MissingPermission")
suspend fun BluetoothAdapter.enableAsync(context: Context): Boolean {
    val deferred = CompletableDeferred<Unit>()
    val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(receiverContext: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_ON) deferred.complete(Unit)
            }
        }
    }
    context.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    if (!isEnabled) {
        try {
            enable()
            withTimeout(5000) { deferred.await() }
        } catch (e: Exception) {
            Log.w(TAG, "Failed enabling Bluetooth")
        }
    }
    context.unregisterReceiver(receiver)
    return isEnabled
}
