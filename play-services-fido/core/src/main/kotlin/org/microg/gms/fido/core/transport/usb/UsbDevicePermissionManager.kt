/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.transport.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.core.app.PendingIntentCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
import kotlinx.coroutines.CompletableDeferred
import java.util.Timer
import java.util.TimerTask

private val Context.usbPermissionCallbackAction
    get() = "$packageName.USB_PERMISSION_CALLBACK"

private object UsbDevicePermissionReceiver : BroadcastReceiver() {
    private var registered = false
    private val pendingRequests = hashMapOf<UsbDevice, MutableList<CompletableDeferred<Boolean>>>()

    fun register(context: Context) = synchronized(this) {
        if (!registered) {
            ContextCompat.registerReceiver(context, this, IntentFilter(context.usbPermissionCallbackAction), RECEIVER_NOT_EXPORTED)
            registered = true
        }
    }

    fun addDeferred(device: UsbDevice, deferred: CompletableDeferred<Boolean>) = synchronized(this) {
        if (pendingRequests.containsKey(device)) {
            pendingRequests[device]!!.add(deferred)
            false
        } else {
            pendingRequests[device] = arrayListOf(deferred)
            true
        }
    }

    fun isDeferred(device: UsbDevice): Boolean = synchronized(this) {
        return pendingRequests.containsKey(device)
    }

    fun unregister(context: Context) = synchronized(this) {
        if (registered) {
            context.unregisterReceiver(this)
            registered = false
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || context == null) return
        if (intent.action == context.usbPermissionCallbackAction) {
            val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
            if (device != null) {
                synchronized(this) {
                    if (pendingRequests.containsKey(device)) {
                        val hasPermission = context.usbManager?.hasPermission(device) == true
                        for (deferred in pendingRequests[device].orEmpty()) {
                            deferred.complete(hasPermission)
                        }
                        pendingRequests.remove(device)
                        if (pendingRequests.isEmpty()) {
                            unregister(context)
                        }
                    }
                }
            }
        }
    }
}

class UsbDevicePermissionManager(private val context: Context) {

    suspend fun awaitPermission(device: UsbDevice): Boolean {
        if (context.usbManager?.hasPermission(device) == true) return true
        val res = CompletableDeferred<Boolean>()
        if (UsbDevicePermissionReceiver.addDeferred(device, res)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Log.d(TAG, "PermissionReceiver added for ${device.productName} (${context.packageName})")
            }
            UsbDevicePermissionReceiver.register(context)
            schedulePermissionRequest(device, 5)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Log.d(TAG, "PermissionReceiver already added for ${device.productName}")
            }
        }
        requestPermission(device)
        return res.await()
    }

    // In case we couldn't ask for permission, retry every secondes, with a maximum of `maxRetries`
    private fun schedulePermissionRequest(device: UsbDevice, maxRetries: Int) {
        if (maxRetries < 1) return
        Timer().schedule(object : TimerTask() {
            override fun run() {
                if (UsbDevicePermissionReceiver.isDeferred(device)) {
                    requestPermission(device)
                    schedulePermissionRequest(device, maxRetries - 1)
                }
            }
        }, 1000)
    }

    private fun requestPermission(device: UsbDevice) {
        val intent = PendingIntentCompat.getBroadcast(context, 0, Intent(context.usbPermissionCallbackAction).apply { `package` = context.packageName }, 0, true)
        context.usbManager?.requestPermission(device, intent)
    }

    companion object {
        private const val TAG = "UsbDevicePermissionMan"
    }
}
