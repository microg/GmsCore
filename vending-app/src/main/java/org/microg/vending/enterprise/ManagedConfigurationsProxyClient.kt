/*
 * SPDX-FileCopyrightText: 2026 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.enterprise

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.util.Log
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal class ManagedConfigurationsProxyClient(private val context: Context) {

    suspend fun applyRestrictions(packageName: String, restrictions: Bundle): Boolean {
        val intent = findProxyIntent()
        if (intent == null) {
            Log.w(TAG, "No device admin offers a managed configurations proxy")
            return false
        }

        return suspendCancellableCoroutine { continuation ->
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder) {
                    val sent = send(service, packageName, restrictions)
                    context.unbindService(this)
                    continuation.resumeOnce(sent)
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    continuation.resumeOnce(false)
                }
            }

            if (!context.bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
                continuation.resumeOnce(false)
            }
        }
    }

    private fun findProxyIntent(): Intent? {
        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        return devicePolicyManager.activeAdmins.orEmpty()
            .map { Intent(ACTION_APPLICATION_RESTRICTIONS_PROXY).setPackage(it.packageName) }
            .firstOrNull { context.packageManager.resolveService(it, 0) != null }
    }

    private fun send(service: IBinder, packageName: String, restrictions: Bundle): Boolean {
        val data = Bundle()
        data.putString(KEY_PACKAGE_NAME, packageName)
        data.putBundle(KEY_APPLICATION_RESTRICTIONS, restrictions)
        data.putBoolean(KEY_SEND_BROADCAST, true)

        val message = Message.obtain(null, MESSAGE_SET_APPLICATION_RESTRICTIONS)
        message.data = data

        return runCatching { Messenger(service).send(message) }
            .onSuccess { Log.i(TAG, "Managed configuration sent for $packageName") }
            .onFailure { Log.w(TAG, "Unable to send the managed configuration for $packageName", it) }
            .isSuccess
    }

    private fun CancellableContinuation<Boolean>.resumeOnce(value: Boolean) {
        if (isActive) {
            resume(value)
        }
    }

    companion object {
        private const val TAG = "GmsVendingManagedConfig"
        private const val ACTION_APPLICATION_RESTRICTIONS_PROXY = "com.android.vending.dpc.APPLICATION_RESTRICTIONS_PROXY"
        private const val MESSAGE_SET_APPLICATION_RESTRICTIONS = 1
        private const val KEY_PACKAGE_NAME = "packageName"
        private const val KEY_APPLICATION_RESTRICTIONS = "applicationRestrictions"
        private const val KEY_SEND_BROADCAST = "sendBroadcast"
    }
}
