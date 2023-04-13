/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.transport.usb

import android.content.Context
import android.hardware.usb.*

val Context.usbManager: UsbManager?
    get() = getSystemService(Context.USB_SERVICE) as? UsbManager?

val UsbDevice.interfaces: Iterable<UsbInterface>
    get() = Iterable {
        object : Iterator<UsbInterface> {
            private var index = 0
            override fun hasNext(): Boolean = index < interfaceCount
            override fun next(): UsbInterface = getInterface(index++)
        }
    }

val UsbInterface.endpoints: Iterable<UsbEndpoint>
    get() = Iterable {
        object : Iterator<UsbEndpoint> {
            private var index = 0
            override fun hasNext(): Boolean = index < endpointCount
            override fun next(): UsbEndpoint = getEndpoint(index++)
        }
    }

inline fun <R> UsbDeviceConnection.use(block: (UsbDeviceConnection) -> R): R {
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

inline fun <R> UsbRequest.initialize(connection: UsbDeviceConnection, endpoint: UsbEndpoint, block: (UsbRequest) -> R): R {
    var exception: Throwable? = null
    try {
        initialize(connection, endpoint)
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
