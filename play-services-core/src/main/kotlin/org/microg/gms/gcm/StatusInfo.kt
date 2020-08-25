/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.gcm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import java.io.Serializable
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val ACTION_STATUS_INFO_REQUEST = "org.microg.gms.STATUS_INFO_REQUEST"
private const val ACTION_STATUS_INFO_RESPONSE = "org.microg.gms.STATUS_INFO_RESPONSE"
private const val EXTRA_STATUS_INFO = "org.microg.gms.STATUS_INFO"
private const val TAG = "GmsGcmStatusInfo"

data class StatusInfo(val connected: Boolean, val startTimestamp: Long) : Serializable

class StatusInfoProvider : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            context.sendOrderedBroadcast(Intent(ACTION_STATUS_INFO_RESPONSE).apply {
                setPackage(context.packageName)
                putExtra(EXTRA_STATUS_INFO, StatusInfo(McsService.isConnected(), McsService.getStartTimestamp()))
            }, null)
        } catch (e: Exception) {
            Log.w(TAG, e)
        }
    }
}

suspend fun getStatusInfo(context: Context): StatusInfo? = suspendCoroutine {
    context.registerReceiver(object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            try {
                it.resume(intent?.getSerializableExtra(EXTRA_STATUS_INFO) as? StatusInfo)
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
            context.unregisterReceiver(this)
        }
    }, IntentFilter(ACTION_STATUS_INFO_RESPONSE))
    try {
        context.sendOrderedBroadcast(Intent(context, StatusInfoProvider::class.java), null)
    } catch (e: Exception) {
        it.resume(null)
    }
}
