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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Serializable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val ACTION_SERVICE_INFO_REQUEST = "org.microg.gms.gcm.SERVICE_INFO_REQUEST"
private const val ACTION_SERVICE_INFO_RESPONSE = "org.microg.gms.gcm.SERVICE_INFO_RESPONSE"
private const val EXTRA_SERVICE_INFO = "org.microg.gms.gcm.SERVICE_INFO"
private const val TAG = "GmsGcmStatusInfo"

data class ServiceInfo(val configuration: ServiceConfiguration, val connected: Boolean, val startTimestamp: Long, val learntMobileInterval: Int, val learntWifiInterval: Int, val learntOtherInterval: Int) : Serializable

data class ServiceConfiguration(val enabled: Boolean, val confirmNewApps: Boolean, val mobile: Int, val wifi: Int, val roaming: Int, val other: Int) : Serializable

private fun GcmPrefs.toConfiguration(): ServiceConfiguration = ServiceConfiguration(isEnabled, confirmNewApps, networkMobile, networkWifi, networkRoaming, networkOther)

class ServiceInfoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            context.sendOrderedBroadcast(Intent(ACTION_SERVICE_INFO_RESPONSE).apply {
                setPackage(context.packageName)
                val prefs = GcmPrefs.get(context)
                val info = ServiceInfo(
                    configuration = prefs.toConfiguration(),
                    connected = McsService.isConnected(context),
                    startTimestamp = McsService.getStartTimestamp(),
                    learntMobileInterval = prefs.learntMobileInterval,
                    learntWifiInterval = prefs.learntWifiInterval,
                    learntOtherInterval = prefs.learntOtherInterval
                )
                putExtra(EXTRA_SERVICE_INFO, info)
            }, null)
        } catch (e: Exception) {
            Log.w(TAG, e)
        }
    }
}

private suspend fun sendToServiceInfoReceiver(intent: Intent, context: Context): ServiceInfo = suspendCoroutine {
    context.registerReceiver(object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            context.unregisterReceiver(this)
            val serviceInfo = try {
                intent.getSerializableExtra(EXTRA_SERVICE_INFO) as ServiceInfo
            } catch (e: Exception) {
                it.resumeWithException(e)
                return
            }
            try {
                it.resume(serviceInfo)
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
    }, IntentFilter(ACTION_SERVICE_INFO_RESPONSE))
    try {
        context.sendOrderedBroadcast(intent, null)
    } catch (e: Exception) {
        it.resumeWithException(e)
    }
}

suspend fun getGcmServiceInfo(context: Context): ServiceInfo = sendToServiceInfoReceiver(
    // this is still using a broadcast, because it calls into McsService in the persistent process
        Intent(context, ServiceInfoReceiver::class.java).apply {
            action = ACTION_SERVICE_INFO_REQUEST
        }, context)

suspend fun setGcmServiceConfiguration(context: Context, configuration: ServiceConfiguration) = withContext(Dispatchers.IO) {
    GcmPrefs.write(context, configuration)
}
