/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.transport.usb

import android.content.Context
import android.hardware.usb.UsbConstants.*
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbInterface
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.android.gms.fido.fido2.api.common.AuthenticatorResponse
import com.google.android.gms.fido.fido2.api.common.RequestOptions
import kotlinx.coroutines.CancellationException
import org.microg.gms.fido.core.transport.Transport
import org.microg.gms.fido.core.transport.TransportHandler

@RequiresApi(21)
class UsbTransportHandler(private val context: Context) : TransportHandler(Transport.USB) {
    override val isSupported: Boolean
        get() = context.packageManager.hasSystemFeature("android.hardware.usb.host") && context.usbManager != null

    private val devicePermissionManager by lazy { UsbDevicePermissionManager(context) }

    private infix fun<T> List<T>.eq(other: List<T>):Boolean = other.size == size && zip(other).all { (x, y) -> x == y }

    suspend fun getCtapHidInterface(device: UsbDevice): UsbInterface? {
        for (iface in device.interfaces) {
            if (iface.interfaceClass != USB_CLASS_HID) continue
            if (iface.interfaceSubclass != 0) continue
            if (iface.interfaceProtocol != 0) continue
            if (iface.endpointCount != 2) continue
            val e0 = iface.getEndpoint(0)
            val e1 = iface.getEndpoint(1)
            val (i, o) = if (e0.direction == USB_DIR_IN) e0 to e1 else e1 to e0
            if (i.type != USB_ENDPOINT_XFER_INT || o.type != USB_ENDPOINT_XFER_INT) continue
            if (i.direction != USB_DIR_IN || o.direction != USB_DIR_OUT) continue

            Log.d(TAG, "${device.productName} has suitable hid interface")
            if (!devicePermissionManager.awaitPermission(device)) continue
            Log.d(TAG, "${device.productName} has permission")
            val match = context.usbManager?.openDevice(device)?.use { connection ->
                if (connection.claimInterface(iface, true)) {
                    val buf = ByteArray(256)
                    val read = connection.controlTransfer(0x81, 0x06, 0x2200, iface.id, buf, buf.size, 5000)
                    read >= 5 && buf.slice(0 until 5) eq CTAPHID_SIGNATURE
                } else {
                    false
                }
            } == true
            if (match) {
                return iface
            }
        }
        return null
    }

    override suspend fun start(options: RequestOptions, callerPackage: String): AuthenticatorResponse {
        for (device in context.usbManager?.deviceList?.values.orEmpty()) {
            val iface = getCtapHidInterface(device)
            if (iface != null) {
                context.usbManager?.openDevice(device)?.use { connection ->
                    if (connection.claimInterface(iface, true)) {

                    }
                }
            }
        }
        Log.d(TAG, "No suitable device found")
        throw CancellationException("No suitable device found")
    }

    companion object {
        const val TAG = "FidoUsbHandler"
        val CTAPHID_SIGNATURE = listOf<Byte>(0x06, 0xd0.toByte(), 0xf1.toByte(), 0x09, 0x01)
    }
}
