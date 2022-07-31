/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.protocol.msgs

class U2fRegistrationCommand(request: U2fRegistrationRequest) :
    Ctap1Command<U2fRegistrationRequest, U2fRegistrationResponse>(request)

class U2fRegistrationRequest(val challenge: ByteArray, val application: ByteArray) : Ctap1Request(0x01, data = challenge + application)

class U2fRegistrationResponse(
    val userPublicKey: ByteArray,
    val keyHandle: ByteArray,
    val attestationCertificate: ByteArray,
    val signature: ByteArray
) : Ctap1Response(0x01)
