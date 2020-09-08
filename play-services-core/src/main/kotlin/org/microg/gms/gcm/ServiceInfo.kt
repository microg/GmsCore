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
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val ACTION_SERVICE_INFO_REQUEST = "org.microg.gms.gcm.SERVICE_INFO_REQUEST"
private const val ACTION_UPDATE_CONFIGURATION = "org.microg.gms.gcm.UPDATE_CONFIGURATION"
private const val ACTION_SERVICE_INFO_RESPONSE = "org.microg.gms.gcm.SERVICE_INFO_RESPONSE"
private const val EXTRA_SERVICE_INFO = "org.microg.gms.gcm.SERVICE_INFO"
private const val EXTRA_CONFIGURATION = "org.microg.gms.gcm.CONFIGURATION"
private const val TAG = "GmsGcmStatusInfo"

data class ServiceInfo(val configuration: ServiceConfiguration, val connected: Boolean, val startTimestamp: Long) : Serializable

// TODO: Intervals
data class ServiceConfiguration(val enabled: Boolean, val confirmNewApps: Boolean) : Serializable {
    fun saveToPrefs(context: Context) {
        GcmPrefs.setEnabled(context, enabled)
        // TODO: confirm new apps
    }
}

private fun GcmPrefs.toConfiguration(): ServiceConfiguration = ServiceConfiguration(isEnabled, isConfirmNewApps)

class ServiceInfoReceiver : BroadcastReceiver() {
    private fun sendInfoResponse(context: Context) {
        context.sendOrderedBroadcast(Intent(ACTION_SERVICE_INFO_RESPONSE).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_SERVICE_INFO, ServiceInfo(GcmPrefs.get(context).toConfiguration(), McsService.isConnected(context), McsService.getStartTimestamp()))
        }, null)
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            when (intent.action) {
                ACTION_UPDATE_CONFIGURATION -> {
                    (intent.getSerializableExtra(EXTRA_CONFIGURATION) as? ServiceConfiguration)?.saveToPrefs(context)
                }
            }
            sendInfoResponse(context)
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
        Intent(context, ServiceInfoReceiver::class.java).apply {
            action = ACTION_SERVICE_INFO_REQUEST
        }, context)

suspend fun setGcmServiceConfiguration(context: Context, configuration: ServiceConfiguration): ServiceInfo = sendToServiceInfoReceiver(
        Intent(context, ServiceInfoReceiver::class.java).apply {
            action = ACTION_UPDATE_CONFIGURATION
            putExtra(EXTRA_CONFIGURATION, configuration)
        }, context)
