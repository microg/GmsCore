/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.transport.usb.ctaphid

import android.util.Base64
import org.microg.gms.fido.core.protocol.msgs.Ctap1Request
import org.microg.gms.fido.core.protocol.msgs.Ctap2Request
import org.microg.gms.utils.toBase64
import kotlin.random.Random

abstract class CtapHidRequest(commandId: Byte, data: ByteArray = ByteArray(0)) : CtapHidMessage(commandId, data) {
    override fun toString(): String =
        "CtapHidRequest(commandId=0x${commandId.toString(16)}, data=${data.toBase64(Base64.NO_WRAP)})"
}

class CtapHidPingRequest(data: ByteArray) : CtapHidRequest(0x01, data) {
    override fun toString(): String = "CtapHidPingRequest(data=${data.toBase64(Base64.NO_WRAP)})"
}

class CtapHidMessageRequest(val request: Ctap1Request) :
    CtapHidRequest(0x03, request.apdu) {
    override fun toString(): String = "CtapHidMessageRequest(${request})"
}

class CtapHidLockRequest(val seconds: Byte) : CtapHidRequest(0x04, byteArrayOf(seconds)) {
    override fun toString(): String = "CtapHidLockRequest(seconds=$seconds)"
}

class CtapHidInitRequest(val nonce: ByteArray = Random.nextBytes(8)) :
    CtapHidRequest(0x06, nonce) {
    init {
        if (nonce.size != 8) throw IllegalArgumentException("nonce must be 8 bytes")
    }

    override fun toString(): String = "CtapHidInitRequest(nonce=${nonce.toBase64(Base64.NO_WRAP)})"
}

class CtapHidWinkRequest : CtapHidRequest(0x08) {
    override fun toString(): String = "CtapHidWinkRequest()"
}

class CtapHidCborRequest(val request: Ctap2Request) :
    CtapHidRequest(0x10, byteArrayOf(request.commandByte) + request.payload) {

    override fun toString(): String = "CtapHidCborRequest(${request})"
}

class CtapHidCancelRequest : CtapHidRequest(0x11) {
    override fun toString(): String = "CtapHidCancelRequest()"
}
