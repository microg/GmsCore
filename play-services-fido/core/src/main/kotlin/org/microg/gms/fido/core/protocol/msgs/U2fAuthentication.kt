/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.protocol.msgs

import android.util.Base64
import org.microg.gms.utils.toBase64
import java.io.DataInputStream
import java.io.InputStream

class U2fAuthenticationCommand(request: U2fAuthenticationRequest) :
    Ctap1Command<U2fAuthenticationRequest, U2fAuthenticationResponse>(request) {
    constructor(controlByte: Byte, challenge: ByteArray, application: ByteArray, keyHandle: ByteArray) :
            this(U2fAuthenticationRequest(controlByte, challenge, application, keyHandle))

    override fun decodeResponse(statusCode: Short, i: InputStream): U2fAuthenticationResponse = U2fAuthenticationResponse.decode(statusCode, i)
}

class U2fAuthenticationRequest(val controlByte: Byte, val challenge: ByteArray, val application: ByteArray, val keyHandle: ByteArray) :
    Ctap1Request(0x02, data = challenge + application + keyHandle.size.toByte() + keyHandle, p1 = controlByte) {
    init {
        require(challenge.size == 32)
        require(application.size == 32)
    }
    override fun toString(): String = "U2fAuthenticationRequest(controlByte=0x${controlByte.toString(16)}, " +
            "challenge=${challenge.toBase64(Base64.NO_WRAP)}, " +
            "application=${application.toBase64(Base64.NO_WRAP)}, " +
            "keyHandle=${keyHandle.toBase64(Base64.NO_WRAP)})"
}

class U2fAuthenticationResponse(
    statusCode: Short,
    val userPresence: Boolean,
    val counter: Int,
    val signature: ByteArray
) : Ctap1Response(statusCode) {
    companion object {
        fun decode(statusCode: Short, i: InputStream): U2fAuthenticationResponse {
            val userPresence = i.read() and 0x1 > 0
            val counter = DataInputStream(i).readInt()
            val signature = i.readBytes()
            return U2fAuthenticationResponse(statusCode, userPresence, counter, signature)
        }
    }
}
