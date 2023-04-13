/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.transport.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants.*
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.android.gms.fido.fido2.api.common.*
import com.upokecenter.cbor.CBORObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import org.microg.gms.fido.core.*
import org.microg.gms.fido.core.protocol.*
import org.microg.gms.fido.core.protocol.msgs.*
import org.microg.gms.fido.core.transport.CtapConnection
import org.microg.gms.fido.core.transport.Transport
import org.microg.gms.fido.core.transport.TransportHandler
import org.microg.gms.fido.core.transport.TransportHandlerCallback
import org.microg.gms.fido.core.transport.usb.ctaphid.CtapHidConnection
import org.microg.gms.fido.core.transport.usb.ctaphid.CtapHidMessageStatusException
import org.microg.gms.utils.toBase64

@RequiresApi(21)
class UsbTransportHandler(private val context: Context, callback: TransportHandlerCallback? = null) :
    TransportHandler(Transport.USB, callback) {
    override val isSupported: Boolean
        get() = context.packageManager.hasSystemFeature("android.hardware.usb.host") && context.usbManager != null

    private val devicePermissionManager by lazy { UsbDevicePermissionManager(context) }

    private var device: UsbDevice? = null

    private infix fun <T> List<T>.eq(other: List<T>): Boolean =
        other.size == size && zip(other).all { (x, y) -> x == y }

    suspend fun getCtapHidInterface(device: UsbDevice): UsbInterface? {
        for (iface in device.interfaces) {
            if (iface.interfaceClass != USB_CLASS_HID) continue
            if (iface.endpointCount != 2) continue
            if (!iface.endpoints.all { it.type == USB_ENDPOINT_XFER_INT }) continue
            if (!iface.endpoints.any { it.direction == USB_DIR_IN }) continue
            if (!iface.endpoints.any { it.direction == USB_DIR_OUT }) continue

            Log.d(TAG, "${device.productName} has suitable hid interface ${iface.id}")
            if (!devicePermissionManager.awaitPermission(device)) continue
            Log.d(TAG, "${device.productName} has permission")
            val match = context.usbManager?.openDevice(device)?.use { connection ->
                if (connection.claimInterface(iface, true)) {
                    val buf = ByteArray(256)
                    val read = connection.controlTransfer(0x81, 0x06, 0x2200, iface.id, buf, buf.size, 5000)
                    Log.d(TAG, "Signature: ${buf.slice(0 until read).toByteArray().toBase64(Base64.NO_WRAP)}")
                    read >= 5 && buf.slice(0 until 5) eq CTAPHID_SIGNATURE
                } else {
                    Log.d(TAG, "Failed claiming interface")
                    false
                }
            } == true
            if (match) {
                return iface
            } else {
                Log.d(TAG, "${device.productName} signature does not match")
            }
        }
        return null
    }

    suspend fun register(
        options: RequestOptions,
        callerPackage: String,
        device: UsbDevice,
        iface: UsbInterface
    ): AuthenticatorAttestationResponse {
        return CtapHidConnection(context, device, iface).open {
            register(it, context, options, callerPackage)
        }
    }

    suspend fun sign(
        options: RequestOptions,
        callerPackage: String,
        device: UsbDevice,
        iface: UsbInterface
    ): AuthenticatorAssertionResponse {
        return CtapHidConnection(context, device, iface).open {
            sign(it, context, options, callerPackage)
        }
    }

    private suspend fun waitForNewUsbDevice(): UsbDevice {
        val deferred = CompletableDeferred<UsbDevice>()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != UsbManager.ACTION_USB_DEVICE_ATTACHED) return
                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE) ?: return
                deferred.complete(device)
            }
        }
        context.registerReceiver(receiver, IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED))
        invokeStatusChanged(TransportHandlerCallback.STATUS_WAITING_FOR_DEVICE)
        val device = deferred.await()
        context.unregisterReceiver(receiver)
        return device
    }

    suspend fun handle(
        options: RequestOptions,
        callerPackage: String,
        device: UsbDevice,
        iface: UsbInterface
    ): AuthenticatorResponse {
        Log.d(TAG, "Trying to use ${device.productName} for ${options.type}")
        invokeStatusChanged(
            TransportHandlerCallback.STATUS_WAITING_FOR_USER,
            Bundle().apply { putParcelable(UsbManager.EXTRA_DEVICE, device) })
        try {
            return when (options.type) {
                RequestOptionsType.REGISTER -> register(options, callerPackage, device, iface)
                RequestOptionsType.SIGN -> sign(options, callerPackage, device, iface)
            }
        } finally {
            this.device = null
        }
    }

    override suspend fun start(options: RequestOptions, callerPackage: String): AuthenticatorResponse {
        for (device in context.usbManager?.deviceList?.values.orEmpty()) {
            val iface = getCtapHidInterface(device) ?: continue
            try {
                return handle(options, callerPackage, device, iface)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
        // None of the already connected devices was suitable, waiting for new device
        while (true) {
            val device = waitForNewUsbDevice()
            val iface = getCtapHidInterface(device) ?: continue
            try {
                return handle(options, callerPackage, device, iface)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
    }

    companion object {
        const val TAG = "FidoUsbHandler"
        val CTAPHID_SIGNATURE = listOf<Byte>(0x06, 0xd0.toByte(), 0xf1.toByte(), 0x09, 0x01)
    }
}
