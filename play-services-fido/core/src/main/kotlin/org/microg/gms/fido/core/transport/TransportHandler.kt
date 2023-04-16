/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.transport

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.android.gms.fido.fido2.api.common.*
import com.google.android.gms.fido.fido2.api.common.UserVerificationRequirement.REQUIRED
import com.upokecenter.cbor.CBORObject
import kotlinx.coroutines.delay
import org.microg.gms.fido.core.*
import org.microg.gms.fido.core.protocol.*
import org.microg.gms.fido.core.protocol.msgs.*
import org.microg.gms.fido.core.transport.nfc.CtapNfcMessageStatusException
import org.microg.gms.fido.core.transport.usb.ctaphid.CtapHidMessageStatusException

abstract class TransportHandler(val transport: Transport, val callback: TransportHandlerCallback?) {
    open val isSupported: Boolean
        get() = false

    open suspend fun start(options: RequestOptions, callerPackage: String): AuthenticatorResponse =
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
        clientDataHash: ByteArray
    ): Pair<AuthenticatorMakeCredentialResponse, ByteArray?> {
        val reqOptions = AuthenticatorMakeCredentialRequest.Companion.Options(
            options.registerOptions.authenticatorSelection?.requireResidentKey == true,
            options.registerOptions.authenticatorSelection?.requireUserVerification == REQUIRED
        )
        val extensions = mutableMapOf<String, CBORObject>()
        if (options.authenticationExtensions?.fidoAppIdExtension?.appId != null) {
            extensions["appidExclude"] =
                options.authenticationExtensions.fidoAppIdExtension.appId.encodeAsCbor()
        }
        if (options.authenticationExtensions?.userVerificationMethodExtension?.uvm != null) {
            extensions["uvm"] =
                options.authenticationExtensions.userVerificationMethodExtension.uvm.encodeAsCbor()
        }
        val request = AuthenticatorMakeCredentialRequest(
            clientDataHash,
            options.registerOptions.rp,
            options.registerOptions.user,
            options.registerOptions.parameters,
            options.registerOptions.excludeList.orEmpty(),
            extensions,
            reqOptions
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
        if (options.registerOptions.authenticatorSelection.requireResidentKey == true)
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
        callerPackage: String
    ): AuthenticatorAttestationResponse {
        val (clientData, clientDataHash) = getClientDataAndHash(context, options, callerPackage)
        val (response, keyHandle) = when {
            connection.hasCtap2Support -> {
                if (connection.hasCtap1Support &&
                    !connection.canMakeCredentialWithoutUserVerification && connection.hasClientPin &&
                    options.registerOptions.authenticatorSelection.requireUserVerification != REQUIRED &&
                    options.registerOptions.authenticatorSelection.requireResidentKey != true
                ) {
                    Log.d(TAG, "Using CTAP1/U2F for PIN-less registration")
                    ctap1register(connection, options, clientDataHash)
                } else {
                    ctap2register(connection, options, clientDataHash)
                }
            }
            connection.hasCtap1Support -> ctap1register(connection, options, clientDataHash)
            else -> throw IllegalStateException()
        }
        return AuthenticatorAttestationResponse(
            keyHandle,
            clientData,
            AnyAttestationObject(response.authData, response.fmt, response.attStmt).encode()
        )
    }


    private suspend fun ctap2sign(
        connection: CtapConnection,
        options: RequestOptions,
        clientDataHash: ByteArray
    ): Pair<AuthenticatorGetAssertionResponse, ByteArray?> {
        val reqOptions = AuthenticatorGetAssertionRequest.Companion.Options(
            userVerification = options.signOptions.requireUserVerification == REQUIRED
        )
        val extensions = mutableMapOf<String, CBORObject>()
        if (options.authenticationExtensions?.fidoAppIdExtension?.appId != null) {
            extensions["appid"] = options.authenticationExtensions.fidoAppIdExtension.appId.encodeAsCbor()
        }
        if (options.authenticationExtensions?.userVerificationMethodExtension?.uvm != null) {
            extensions["uvm"] =
                options.authenticationExtensions.userVerificationMethodExtension.uvm.encodeAsCbor()
        }
        val request = AuthenticatorGetAssertionRequest(
            options.rpId,
            clientDataHash,
            options.signOptions.allowList,
            extensions,
            reqOptions
        )
        val ctap2Response = connection.runCommand(AuthenticatorGetAssertionCommand(request))
        return ctap2Response to ctap2Response.credential?.id
    }

    private suspend fun ctap1sign(
        connection: CtapConnection,
        options: RequestOptions,
        clientDataHash: ByteArray,
        rpIdHash: ByteArray
    ): Pair<AuthenticatorGetAssertionResponse, ByteArray> {
        val cred = options.signOptions.allowList.firstOrNull { cred ->
            ctap1DeviceHasCredential(connection, clientDataHash, rpIdHash, cred)
        } ?: options.signOptions.allowList.first()

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
                    val appIdHash = options.authenticationExtensions.fidoAppIdExtension.appId.toByteArray()
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
        callerPackage: String
    ): AuthenticatorAssertionResponse {
        val (clientData, clientDataHash) = getClientDataAndHash(context, options, callerPackage)
        val (response, credentialId) = when {
            connection.hasCtap2Support -> {
                try {
                    ctap2sign(connection, options, clientDataHash)
                } catch (e: Ctap2StatusException) {
                    if (e.status == 0x2e.toByte() &&
                        connection.hasCtap1Support && connection.hasClientPin &&
                        options.signOptions.allowList.isNotEmpty() &&
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
            credentialId,
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
