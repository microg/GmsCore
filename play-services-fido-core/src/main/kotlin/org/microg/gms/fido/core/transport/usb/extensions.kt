/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.transport.usb

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager

val Context.usbManager: UsbManager?
    get() = getSystemService(Context.USB_SERVICE) as? UsbManager?

val UsbDevice.interfaces: Iterable<UsbInterface>
    get() = Iterable {
        object : Iterator<UsbInterface> {
            private var index = 0
            override fun hasNext(): Boolean = index + 1 < interfaceCount
            override fun next(): UsbInterface = getInterface(index++)
        }
    }

fun <R> UsbDeviceConnection.use(block: (UsbDeviceConnection) -> R): R {
    var exception: Throwable? = null
    try {
        return block(this)
    } catch (e: Throwable) {
        exception = e
        throw e
    } finally {
        when (exception) {
            null -> close()
            else -> try {
                close()
            } catch (closeException: Throwable) {
                // cause.addSuppressed(closeException) // ignored here
            }
        }
    }
}
