package org.microg.gms.fido.core.protocol.msgs

import com.upokecenter.cbor.CBORObject
import org.microg.gms.fido.core.protocol.CoseKey
import org.microg.gms.fido.core.protocol.decodeAsCoseKey
import org.microg.gms.fido.core.protocol.encodeAsCbor

class AuthenticatorClientPINCommand(request: AuthenticatorClientPINRequest) :
    Ctap2Command<AuthenticatorClientPINRequest, AuthenticatorClientPINResponse>(request) {

    override fun decodeResponse(obj: CBORObject) = AuthenticatorClientPINResponse.decodeFromCbor(obj)

    override val timeout: Long
        get() = 60000

}

class AuthenticatorClientPINRequest(
    val pinProtocol: Int,
    val subCommand: Int,
    val keyAgreement: CoseKey? = null,
    val pinAuth: ByteArray? = null,
    val newPinEnc: ByteArray? = null,
    val pinHashEnc: ByteArray? = null
) : Ctap2Request(0x06, CBORObject.NewMap().apply {
    set(0x01, pinProtocol.encodeAsCbor())
    set(0x02, subCommand.encodeAsCbor())
    if (keyAgreement != null) set(0x03, keyAgreement.encodeAsCbor())
    if (pinAuth != null) set(0x04, pinAuth.encodeAsCbor())
    if (newPinEnc != null) set(0x05, newPinEnc.encodeAsCbor())
    if (pinHashEnc != null) set(0x06, pinHashEnc.encodeAsCbor())
}) {
    override fun toString(): String {
        return "AuthenticatorClientPINRequest(pinProtocol=$pinProtocol, " +
                "subCommand=$subCommand, keyAgreement=$keyAgreement, " +
                "pinAuth=${pinAuth?.contentToString()}, " +
                "newPinEnc=${newPinEnc?.contentToString()}, " +
                "pinHashEnc=${pinHashEnc?.contentToString()})"
    }

    companion object {
        // PIN protocol versions
        const val PIN_PROTOCOL_VERSION_ONE = 0x01

        // PIN subcommands
        const val GET_RETRIES = 0x01
        const val GET_KEY_AGREEMENT = 0x02
        const val SET_PIN = 0x03
        const val CHANGE_PIN = 0x04
        const val GET_PIN_TOKEN = 0x05
    }
}

class AuthenticatorClientPINResponse(
    val keyAgreement: CoseKey?,
    val pinToken: ByteArray?,
    val retries: Int?
) : Ctap2Response {
    companion object {
        fun decodeFromCbor(obj: CBORObject) = AuthenticatorClientPINResponse(
            obj.get(0x01)?.decodeAsCoseKey(),
            obj.get(0x02)?.GetByteString(),
            obj.get(0x03)?.AsInt32Value()
        )
    }
}