/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.transport.nfc

import android.content.Context
import android.nfc.NfcAdapter
import org.microg.gms.fido.core.transport.Transport
import org.microg.gms.fido.core.transport.TransportHandler
import org.microg.gms.fido.core.transport.TransportHandlerCallback

class NfcTransportHandler(private val context: Context, callback: TransportHandlerCallback? = null) :
    TransportHandler(Transport.NFC, callback) {
    override val isSupported: Boolean
        get() = NfcAdapter.getDefaultAdapter(context) != null
}
