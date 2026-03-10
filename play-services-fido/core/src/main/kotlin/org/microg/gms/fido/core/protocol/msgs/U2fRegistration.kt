/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.protocol.msgs

import android.util.Base64
import org.microg.gms.utils.toBase64
import java.io.InputStream
import javax.security.cert.X509Certificate

class U2fRegistrationCommand(request: U2fRegistrationRequest) :
    Ctap1Command<U2fRegistrationRequest, U2fRegistrationResponse>(request) {
    constructor(challenge: ByteArray, application: ByteArray) : this(U2fRegistrationRequest(challenge, application))

    override fun decodeResponse(statusCode: Short, i: InputStream): U2fRegistrationResponse = U2fRegistrationResponse.decode(statusCode, i)
}

class U2fRegistrationRequest(val challenge: ByteArray, val application: ByteArray) :
    Ctap1Request(0x01, data = challenge + application) {
    init {
        require(challenge.size == 32)
        require(application.size == 32)
    }
    override fun toString(): String = "U2fRegistrationRequest(challenge=${challenge.toBase64(Base64.NO_WRAP)}, " +
            "application=${application.toBase64(Base64.NO_WRAP)})"
}

class U2fRegistrationResponse(
    statusCode: Short,
    val userPublicKey: ByteArray,
    val keyHandle: ByteArray,
    val attestationCertificate: ByteArray,
    val signature: ByteArray
) : Ctap1Response(statusCode) {
    companion object {
        fun decode(statusCode: Short, i: InputStream): U2fRegistrationResponse {
            require(i.read() == 0x05) // reserved byte
            val userPublicKey = ByteArray(65)
            i.read(userPublicKey)
            val keyHandle = ByteArray(i.read())
            i.read(keyHandle)
            val attestationCertificate = X509Certificate.getInstance(i).encoded
            val signature = i.readBytes()
            return U2fRegistrationResponse(statusCode, userPublicKey, keyHandle, attestationCertificate, signature)
        }
    }
}
