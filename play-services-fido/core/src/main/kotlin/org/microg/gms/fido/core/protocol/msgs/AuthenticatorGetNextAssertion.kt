package org.microg.gms.fido.core.protocol.msgs

import com.upokecenter.cbor.CBORObject

class AuthenticatorGetNextAssertionCommand(request: AuthenticatorGetNextAssertionRequest) :
    Ctap2Command<AuthenticatorGetNextAssertionRequest, AuthenticatorGetAssertionResponse>(request) {
    override fun decodeResponse(obj: CBORObject) = AuthenticatorGetAssertionResponse.decodeFromCbor(obj)
    override val timeout: Long
        get() = 60000
}

class AuthenticatorGetNextAssertionRequest() : Ctap2Request(0x08)