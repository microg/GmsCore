/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.transport.usb.ctaphid

import org.microg.gms.fido.core.protocol.msgs.Ctap1Request
import org.microg.gms.fido.core.protocol.msgs.Ctap2Request
import kotlin.math.min
import kotlin.random.Random

abstract class CtapHidRequest(commandId: Byte, data: ByteArray = ByteArray(0)): CtapHidMessage(commandId, data)

class CtapHidPingRequest(data: ByteArray) : CtapHidRequest(0x01, data)

class CtapHidMessageRequest(val request: Ctap1Request) :
    CtapHidRequest(0x03, byteArrayOf(request.commandByte) + request.payload)


class CtapHidLockRequest(val seconds: Byte) : CtapHidRequest(0x04, byteArrayOf(seconds))

class CtapHidInitRequest(val nonce: ByteArray = Random.nextBytes(8)) :
    CtapHidRequest(0x06, nonce) {
    init {
        if (nonce.size != 8) throw IllegalArgumentException("nonce must be 8 bytes")
    }
}

class CtapHidWinkRequest : CtapHidRequest(0x08)

class CtapHidCborRequest(val request: Ctap2Request) :
    CtapHidRequest(0x10, byteArrayOf(request.commandByte) + request.payload)

class CtapHidCancelRequest : CtapHidRequest(0x11)
