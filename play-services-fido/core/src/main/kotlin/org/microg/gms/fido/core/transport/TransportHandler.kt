/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.transport

import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.android.gms.fido.fido2.api.common.*
import com.google.android.gms.fido.fido2.api.common.ResidentKeyRequirement.*
import com.google.android.gms.fido.fido2.api.common.UserVerificationRequirement.*
import com.upokecenter.cbor.CBORObject
import kotlinx.coroutines.delay
import org.microg.gms.fido.core.*
import org.microg.gms.fido.core.protocol.*
import org.microg.gms.fido.core.protocol.CoseKey.Companion.toByteArray
import org.microg.gms.fido.core.protocol.msgs.*
import org.microg.gms.fido.core.transport.nfc.CtapNfcMessageStatusException
import org.microg.gms.fido.core.transport.usb.ctaphid.CtapHidMessageStatusException
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

abstract class TransportHandler(val transport: Transport, val callback: TransportHandlerCallback?) {
    open val isSupported: Boolean
        get() = false

    open suspend fun start(options: RequestOptions, callerPackage: String, pinRequested: Boolean = false, pin: String? = null): AuthenticatorResponse =
        throw RequestHandlingException(ErrorCode.NOT_SUPPORTED_ERR)

    open fun shouldBeUsedInstantly(options: RequestOptions): Boolean = false
    fun invokeStatusChanged(status: String, extras: Bundle? = null) =
        callback?.onStatusChanged(transport, status, extras)

    private suspend fun ctap1DeviceHasCredential(
        connection: CtapConnection,
        challenge: ByteArray,
        application: ByteArray,
        descriptor: PublicKeyCredentialDescriptor
    ): Boolean {
        try {
            connection.runCommand(U2fAuthenticationCommand(0x07, challenge, application, descriptor.id))
            return true
        } catch (e: CtapHidMessageStatusException) {
            return e.status == 0x6985;
        } catch (e: CtapNfcMessageStatusException) {
            return e.status == 0x6985;
        }
    }

    private suspend fun ctap2register(
        connection: CtapConnection,
        options: RequestOptions,
        clientDataHash: ByteArray,
        requireResidentKey: Boolean,
        requireUserVerification: Boolean,
        pinToken: ByteArray? = null
    ): Pair<AuthenticatorMakeCredentialResponse, ByteArray?> {

        // The CTAP2 spec states that the requireUserVerification option from WebAuthn should map
        // to the "uv" option OR the pinAuth/pinProtocl options in the CTAP standard.
        // https://fidoalliance.org/specs/fido-v2.0-ps-20190130/fido-client-to-authenticator-protocol-v2.0-ps-20190130.html#authenticatorGetInfo
        // Later drafts of the standard are much more explicit about this, and state that platforms
        // MUST NOT include the "uv" option key if the authenticator does not support built-in
        // verification, and that they MUST NOT include both the "uv" option key and the pinUvAuth
        // parameter in the same request
        // https://fidoalliance.org/specs/fido-v2.2-rd-20230321/fido-client-to-authenticator-protocol-v2.2-rd-20230321.html#authenticatorMakeCredential
        val ctap2RequireVerification = requireUserVerification && (pinToken == null)

        val reqOptions = AuthenticatorMakeCredentialRequest.Companion.Options(
            requireResidentKey,
            ctap2RequireVerification
        )

        val extensions = mutableMapOf<String, CBORObject>()
        if (options.authenticationExtensions?.fidoAppIdExtension?.appId != null) {
            extensions["appidExclude"] =
                options.authenticationExtensions!!.fidoAppIdExtension!!.appId.encodeAsCbor()
        }
        if (options.authenticationExtensions?.userVerificationMethodExtension?.uvm != null) {
            extensions["uvm"] =
                options.authenticationExtensions!!.userVerificationMethodExtension!!.uvm.encodeAsCbor()
        }

        var pinProtocol: Int? = null
        var pinHashEnc: ByteArray? = null
        if (pinToken != null) {
            val secretKeySpec = SecretKeySpec(pinToken, "HmacSHA256")
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(secretKeySpec)
            pinHashEnc = mac.doFinal(clientDataHash).sliceArray(IntRange(0, 15))
            pinProtocol = 1
        }

        val request = AuthenticatorMakeCredentialRequest(
            clientDataHash,
            options.registerOptions.rp,
            options.registerOptions.user,
            options.registerOptions.parameters,
            options.registerOptions.excludeList.orEmpty(),
            extensions,
            reqOptions,
            pinHashEnc,
            pinProtocol
        )
        val response = connection.runCommand(AuthenticatorMakeCredentialCommand(request))
        val credentialId = AuthenticatorData.decode(response.authData).attestedCredentialData?.id
        return response to credentialId
    }

    private suspend fun ctap1register(
        connection: CtapConnection,
        options: RequestOptions,
        clientDataHash: ByteArray
    ): Pair<AuthenticatorMakeCredentialResponse, ByteArray> {
        val rpIdHash = options.rpId.toByteArray().digest("SHA-256")
        val appIdHash =
            options.authenticationExtensions?.fidoAppIdExtension?.appId?.toByteArray()?.digest("SHA-256")
        if (!options.registerOptions.parameters.isNullOrEmpty() && options.registerOptions.parameters.all { it.algorithmIdAsInteger != -7 })
            throw IllegalArgumentException("Can't use CTAP1 protocol for non ES256 requests")
        if (options.registerOptions.authenticatorSelection?.requireResidentKey == true)
            throw IllegalArgumentException("Can't use CTAP1 protocol when resident key required")
        val hasCredential = options.registerOptions.excludeList.orEmpty().any { cred ->
            ctap1DeviceHasCredential(connection, clientDataHash, rpIdHash, cred) ||
                    if (appIdHash != null) {
                        ctap1DeviceHasCredential(connection, clientDataHash, appIdHash, cred)
                    } else {
                        false
                    }
        }
        while (true) {
            try {
                val response = connection.runCommand(U2fRegistrationCommand(clientDataHash, rpIdHash))
                if (hasCredential) throw RequestHandlingException(
                    ErrorCode.NOT_ALLOWED_ERR,
                    "An excluded credential has already been registered with the device"
                )
                require(response.userPublicKey[0] == 0x04.toByte())
                val coseKey = CoseKey(
                    EC2Algorithm.ES256,
                    response.userPublicKey.sliceArray(1 until 33),
                    response.userPublicKey.sliceArray(33 until 65),
                    1
                )
                val credentialData =
                    AttestedCredentialData(ByteArray(16), response.keyHandle, coseKey.encode())
                val authData = AuthenticatorData(
                    options.rpId.toByteArray().digest("SHA-256"),
                    true,
                    false,
                    0,
                    credentialData
                )
                val attestationObject = if (options.registerOptions.skipAttestation) {
                    NoneAttestationObject(authData)
                } else {
                    FidoU2fAttestationObject(authData, response.signature, response.attestationCertificate)
                }
                val ctap2Response = AuthenticatorMakeCredentialResponse(
                    authData.encode(),
                    attestationObject.fmt,
                    attestationObject.attStmt
                )
                return ctap2Response to response.keyHandle
            } catch (e: CtapHidMessageStatusException) {
                if (e.status != 0x6985) {
                    throw e
                }
            }
            delay(100)
        }
    }

    internal suspend fun register(
        connection: CtapConnection,
        context: Context,
        options: RequestOptions,
        callerPackage: String,
        pinRequested: Boolean,
        pin: String?
    ): AuthenticatorAttestationResponse {
        val (clientData, clientDataHash) = getClientDataAndHash(context, options, callerPackage)

        val requireResidentKey = when (options.registerOptions.authenticatorSelection?.residentKeyRequirement) {
            RESIDENT_KEY_REQUIRED -> true
            RESIDENT_KEY_PREFERRED -> connection.hasResidentKey
            RESIDENT_KEY_DISCOURAGED -> false
            // If residentKeyRequirement is not set, use the value for requireResidentKey
            // Default value for requireResidentKey is false
            else -> options.registerOptions.authenticatorSelection?.requireResidentKey == true
        }

        val requireUserVerification = when(options.registerOptions.authenticatorSelection?.requireUserVerification) {
            REQUIRED -> true
            DISCOURAGED -> false
            // PREFERRED is the default, according to the standard
            // https://www.w3.org/TR/webauthn-3/#dom-authenticatorselectioncriteria-userverification
            // If preferred, only return true if connection is capable of user verification
            else -> connection.hasClientPin || connection.hasUserVerificationSupport
        }
        // If the authenticator has a built-in verification method, let that take precedence over
        // client PIN
        val requiresPin = requireUserVerification && !connection.hasUserVerificationSupport && connection.hasClientPin

        val (response, keyHandle) = when {
            connection.hasCtap2Support && (requireResidentKey || requiresPin) -> {
                try {
                    var pinToken: ByteArray? = null

                    // If we previously requested a pin and the user cancelled it (ie. pinRequested
                    // is true and pin is still null), don't throw the exception, and pass the request
                    // to the authenticator without a pin.
                    if (requiresPin && !pinRequested && pin == null) {
                        throw MissingPinException()
                    }

                    if (requiresPin && pin != null && SDK_INT >= 23) {
                        pinToken = ctap2getPinToken(connection, pin)
                    }

                    // Authenticators seem to give a response even without a PIN token, so we'll allow
                    // the client to call this even without having a PIN token set
                    ctap2register(connection, options, clientDataHash, requireResidentKey, requireUserVerification, pinToken)
                } catch (e: Ctap2StatusException) {
                    if (e.status == 0x36.toByte()) {
                        throw MissingPinException()
                    } else if (e.status == 0x31.toByte()) {
                        throw WrongPinException()
                    } else {
                        throw e
                    }
                }
            }
            connection.hasCtap1Support -> ctap1register(connection, options, clientDataHash)
            else -> throw IllegalStateException()
        }
        return AuthenticatorAttestationResponse(
            keyHandle ?: ByteArray(0).also { Log.w(TAG, "keyHandle was null") },
            clientData,
            AnyAttestationObject(response.authData, response.fmt, response.attStmt).encode(),
            connection.transports.toTypedArray()
        )
    }


    private suspend fun ctap2sign(
        connection: CtapConnection,
        options: RequestOptions,
        clientDataHash: ByteArray,
        requireUserVerification: Boolean,
        pinToken: ByteArray? = null
    ): Pair<AuthenticatorGetAssertionResponse, ByteArray?> {
        val reqOptions = AuthenticatorGetAssertionRequest.Companion.Options(
            // The specification states that the WebAuthn requireUserVerification option should map to
            // the CTAP2 "uv" flag OR pinAuth/pinProtocol. Therefore, set this flag to false if
            // a pinToken is present
            userVerification = requireUserVerification && (pinToken == null)
        )
        val extensions = mutableMapOf<String, CBORObject>()
        if (options.authenticationExtensions?.fidoAppIdExtension?.appId != null) {
            extensions["appid"] = options.authenticationExtensions!!.fidoAppIdExtension!!.appId.encodeAsCbor()
        }
        if (options.authenticationExtensions?.userVerificationMethodExtension?.uvm != null) {
            extensions["uvm"] =
                options.authenticationExtensions!!.userVerificationMethodExtension!!.uvm.encodeAsCbor()
        }

        var pinProtocol: Int? = null
        var pinHashEnc: ByteArray? = null
        if (pinToken != null) {
            val secretKeySpec = SecretKeySpec(pinToken, "HmacSHA256")
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(secretKeySpec)
            pinHashEnc = mac.doFinal(clientDataHash).sliceArray(IntRange(0, 15))
            pinProtocol = 1
        }

        val request = AuthenticatorGetAssertionRequest(
            options.rpId,
            clientDataHash,
            options.signOptions.allowList.orEmpty(),
            extensions,
            reqOptions,
            pinHashEnc,
            pinProtocol
        )
        val ctap2Response = connection.runCommand(AuthenticatorGetAssertionCommand(request))
        return ctap2Response to ctap2Response.credential?.id
    }

    @RequiresApi(23)
    private suspend fun ctap2getPinToken(
        connection: CtapConnection,
        pin: String
    ): ByteArray? {
        // Ask for shared secret from authenticator
        val sharedSecretRequest = AuthenticatorClientPINRequest(
            AuthenticatorClientPINRequest.PIN_PROTOCOL_VERSION_ONE,
            AuthenticatorClientPINRequest.GET_KEY_AGREEMENT
        )
        val sharedSecretResponse = connection.runCommand(AuthenticatorClientPINCommand(sharedSecretRequest))

        if (sharedSecretResponse.keyAgreement == null) {
            return null;
        }

        val x = sharedSecretResponse.keyAgreement.x
        val y = sharedSecretResponse.keyAgreement.y

        val curveName = when (sharedSecretResponse.keyAgreement.curveId) {
            1 -> "secp256r1"
            2 -> "secp384r1"
            3 -> "secp521r1"
            4 -> "x25519"
            5 -> "x448"
            6 -> "Ed25519"
            7 -> "Ed448"
            else -> return null
        }

        // Perform Diffie Hellman key generation
        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC)
        generator.initialize(ECGenParameterSpec(curveName))

        val myKeyPair = generator.generateKeyPair()
        val parameters = AlgorithmParameters.getInstance("EC")
        parameters.init(ECGenParameterSpec(curveName))
        val parameterSpec = parameters.getParameterSpec(ECParameterSpec::class.java)
        val serverKey = KeyFactory.getInstance("EC")
            .generatePublic(ECPublicKeySpec(ECPoint(BigInteger(1, x), BigInteger(1, y)), parameterSpec))
        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(myKeyPair.private)
        keyAgreement.doPhase(serverKey, true)

        // We get the key for the encryption used between the client and the platform by doing an
        // SHA 256 hash of the shared secret
        val sharedSecret = keyAgreement.generateSecret()
        val hash = MessageDigest.getInstance("SHA-256")
        hash.update(sharedSecret)
        val sharedKey = SecretKeySpec(hash.digest(), "AES")

        // Hash the PIN, and then encrypt the first 16 bytes of the hash using the shared key
        val pinHash = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray(StandardCharsets.UTF_8))
        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, sharedKey, IvParameterSpec(ByteArray(16)))
        val pinHashEnc = cipher.doFinal(pinHash.sliceArray(IntRange(0,15)))

        // Now, send back the encrypted pin hash, as well as the public portion of our key so
        // the authenticator also may perform Diffie Hellman
        val publicKey = myKeyPair.public
        if (publicKey !is ECPublicKey) {
            return null
        }
        val coseKey = CoseKey(
            sharedSecretResponse.keyAgreement.algorithm,
            publicKey.w.affineX.toByteArray(32),
            publicKey.w.affineY.toByteArray(32),
            sharedSecretResponse.keyAgreement.curveId
        )

        val pinTokenRequest = AuthenticatorClientPINRequest(
            AuthenticatorClientPINRequest.PIN_PROTOCOL_VERSION_ONE,
            AuthenticatorClientPINRequest.GET_PIN_TOKEN,
            coseKey,
            pinHashEnc = pinHashEnc
        )

        // The pin token is returned to us in encrypted form. Decrypt it, so we may use it when HMAC
        // signing later
        val pinTokenResponse = connection.runCommand(AuthenticatorClientPINCommand(pinTokenRequest))
        cipher.init(Cipher.DECRYPT_MODE, sharedKey, IvParameterSpec(ByteArray(16)))
        return cipher.doFinal(pinTokenResponse.pinToken)
    }

    private suspend fun ctap1sign(
        connection: CtapConnection,
        options: RequestOptions,
        clientDataHash: ByteArray,
        rpIdHash: ByteArray
    ): Pair<AuthenticatorGetAssertionResponse, ByteArray> {
        val cred = options.signOptions.allowList.orEmpty().firstOrNull { cred ->
            ctap1DeviceHasCredential(connection, clientDataHash, rpIdHash, cred)
        } ?: options.signOptions.allowList!!.first()

        while (true) {
            try {
                val response = connection.runCommand(U2fAuthenticationCommand(0x03, clientDataHash, rpIdHash, cred.id))
                val authData = AuthenticatorData(rpIdHash, response.userPresence, false, response.counter)
                val ctap2Response = AuthenticatorGetAssertionResponse(
                    cred,
                    authData.encode(),
                    response.signature,
                    null,
                    null
                )
                return ctap2Response to cred.id
            } catch (e: CtapHidMessageStatusException) {
                if (e.status != 0x6985) {
                    throw e
                }
                delay(100)
            }
        }
    }

    private suspend fun ctap1sign(
        connection: CtapConnection,
        options: RequestOptions,
        clientDataHash: ByteArray
    ): Pair<AuthenticatorGetAssertionResponse, ByteArray> {
        try {
            val rpIdHash = options.rpId.toByteArray().digest("SHA-256")
            return ctap1sign(connection, options, clientDataHash, rpIdHash)
        } catch (e: Exception) {
            try {
                if (options.authenticationExtensions?.fidoAppIdExtension?.appId != null) {
                    val appIdHash = options.authenticationExtensions!!.fidoAppIdExtension!!.appId.toByteArray()
                        .digest("SHA-256")
                    return ctap1sign(connection, options, clientDataHash, appIdHash)
                }
            } catch (e2: Exception) {
            }
            // Throw original
            throw e
        }
    }

    internal suspend fun sign(
        connection: CtapConnection,
        context: Context,
        options: RequestOptions,
        callerPackage: String,
        pinRequested: Boolean,
        pin: String?
    ): AuthenticatorAssertionResponse {
        val (clientData, clientDataHash) = getClientDataAndHash(context, options, callerPackage)

        val (response, credentialId) = when {
            connection.hasCtap2Support -> {
                try {
                    var pinToken: ByteArray? = null

                    val requireUserVerification = when(options.signOptions.requireUserVerification) {
                        REQUIRED -> true
                        DISCOURAGED -> false
                        // PREFERRED is the default, according to the standard
                        // https://www.w3.org/TR/webauthn-3/#dom-authenticatorselectioncriteria-userverification
                        else -> {
                            // If preferred, only return true if connection is capable of user verification
                            connection.hasClientPin || connection.hasUserVerificationSupport
                        }
                    }
                    // If the authenticator has built in user verification, let that take precedence
                    // over PIN verification
                    val requiresPin = requireUserVerification && !connection.hasUserVerificationSupport && connection.hasClientPin

                    // If we require a PIN, throw an exception up to the AuthenticatorActivity
                    // However, if we've already asked the user for a PIN and the user cancelled
                    // (ie. pinRequested is true), continue without asking
                    if (requiresPin && !pinRequested && pin == null) {
                        throw MissingPinException()
                    }

                    if (requiresPin && pin != null && SDK_INT >= 23) {
                        pinToken = ctap2getPinToken(connection, pin)
                    }

                    // Authenticators seem to give a response even without a PIN token, so we'll allow
                    // the client to call this even without having a PIN token set
                    ctap2sign(connection, options, clientDataHash, requireUserVerification, pinToken)
                } catch (e: Ctap2StatusException) {
                    if (e.status == 0x31.toByte()) {
                        throw WrongPinException()
                    } else if (e.status == 0x36.toByte()) {
                        throw MissingPinException()
                    } else if (e.status == 0x2e.toByte() &&
                        connection.hasCtap1Support && connection.hasClientPin &&
                        options.signOptions.allowList.orEmpty().isNotEmpty() &&
                        options.signOptions.requireUserVerification != REQUIRED
                    ) {
                        Log.d(TAG, "Falling back to CTAP1/U2F")
                        try {
                            ctap1sign(connection, options, clientDataHash)
                        } catch (e2: Exception) {
                            // Throw original exception
                            throw e
                        }
                    } else {
                        throw e
                    }
                }
            }
            connection.hasCtap1Support -> ctap1sign(connection, options, clientDataHash)
            else -> throw IllegalStateException()
        }
        return AuthenticatorAssertionResponse(
            credentialId ?: ByteArray(0).also { Log.w(TAG, "keyHandle was null") },
            clientData,
            response.authData,
            response.signature,
            null
        )
    }

    companion object {
        const val TAG = "FidoTransportHandler"
    }
}

interface TransportHandlerCallback {
    fun onStatusChanged(transport: Transport, status: String, extras: Bundle? = null)

    companion object {
        @JvmStatic
        val STATUS_WAITING_FOR_DEVICE = "waiting-for-device"

        @JvmStatic
        val STATUS_WAITING_FOR_USER = "waiting-for-user"

        @JvmStatic
        val STATUS_UNKNOWN = "unknown"
    }
}
