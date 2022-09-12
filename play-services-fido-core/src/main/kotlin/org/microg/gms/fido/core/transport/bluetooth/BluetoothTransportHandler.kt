/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.transport.bluetooth

import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import org.microg.gms.fido.core.transport.Transport
import org.microg.gms.fido.core.transport.TransportHandler
import org.microg.gms.fido.core.transport.TransportHandlerCallback

class BluetoothTransportHandler(private val context: Context, callback: TransportHandlerCallback? = null) :
    TransportHandler(Transport.BLUETOOTH, callback) {
    override val isSupported: Boolean
        get() = Build.VERSION.SDK_INT >= 18 && context.getSystemService<BluetoothManager>()?.adapter != null
}
