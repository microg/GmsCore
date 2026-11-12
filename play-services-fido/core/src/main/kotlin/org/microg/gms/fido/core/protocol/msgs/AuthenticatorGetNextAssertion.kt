/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.protocol.msgs

import com.upokecenter.cbor.CBORObject

class AuthenticatorGetNextAssertionCommand :
    Ctap2Command<AuthenticatorGetNextAssertionRequest, AuthenticatorGetAssertionResponse>(
        AuthenticatorGetNextAssertionRequest()
    ) {
    override fun decodeResponse(obj: CBORObject) = AuthenticatorGetAssertionResponse.decodeFromCbor(obj)
    override val timeout: Long
        get() = 60000
}

class AuthenticatorGetNextAssertionRequest :
    Ctap2Request(Ctap2CommandCode.AuthenticatorGetNextAssertion, null) {
    override fun toString() = "AuthenticatorGetNextAssertionRequest"
}
