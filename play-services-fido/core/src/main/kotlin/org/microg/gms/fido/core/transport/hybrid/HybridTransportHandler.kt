/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.transport.hybrid

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse
import com.google.android.gms.fido.fido2.api.common.RequestOptions
import com.google.android.gms.fido.fido2.api.common.UserVerificationRequirement
import com.upokecenter.cbor.CBORObject
import org.microg.gms.fido.core.RequestOptionsType
import org.microg.gms.fido.core.getClientDataAndHash
import org.microg.gms.fido.core.hybrid.controller.HybridClientController
import org.microg.gms.fido.core.hybrid.generateEcKeyPair
import org.microg.gms.fido.core.hybrid.model.QrCodeData
import org.microg.gms.fido.core.protocol.AnyAttestationObject
import org.microg.gms.fido.core.protocol.AuthenticatorData
import org.microg.gms.fido.core.protocol.msgs.AuthenticatorGetAssertionRequest
import org.microg.gms.fido.core.protocol.msgs.AuthenticatorGetAssertionResponse
import org.microg.gms.fido.core.protocol.msgs.AuthenticatorMakeCredentialRequest
import org.microg.gms.fido.core.protocol.msgs.AuthenticatorMakeCredentialResponse
import org.microg.gms.fido.core.registerOptions
import org.microg.gms.fido.core.signOptions
import org.microg.gms.fido.core.transport.AuthenticatorResponseWithUser
import org.microg.gms.fido.core.transport.Transport
import org.microg.gms.fido.core.transport.TransportHandler
import org.microg.gms.fido.core.transport.TransportHandlerCallback
import org.microg.gms.fido.core.type

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class HybridTransportHandler(private val context: Context, callback: TransportHandlerCallback? = null) :
    TransportHandler(Transport.HYBRID, callback) {
    override val isSupported: Boolean
        get() = context.getSystemService<BluetoothManager>()?.adapter != null

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override suspend fun start(
        options: RequestOptions, callerPackage: String, pinRequested: Boolean, pin: String?, credentialIdString: String?
    ): AuthenticatorResponseWithUser<*> {
        val staticKey = generateEcKeyPair()
        val hybridClientController = HybridClientController(context, staticKey)
        try {
            callback?.onStatusChanged(
                Transport.HYBRID, "QR_CODE_READY",
                bundleOf("qrCodeBitmap" to QrCodeData.generateQrCode(staticKey.first, options.challenge))
            )
            val eid = hybridClientController.startBluetoothScan(options.challenge)
            callback?.onStatusChanged(Transport.HYBRID, "CONNECTING", null)

            val (clientData, clientDataHash) = getClientDataAndHash(context, options, callerPackage)
            val tunnelResp = hybridClientController.startClientTunnel(eid, options.challenge) {
                Log.d(TAG, "start: options: $options")
                val request = when (options.type) {
                    RequestOptionsType.REGISTER -> {
                        val reqOptions = options.registerOptions.authenticatorSelection?.let {
                            val rk = (it.requireResidentKey == true || it.residentKeyRequirement?.toString() == UserVerificationRequirement.REQUIRED.name)
                            val uv = (it.requireUserVerification == UserVerificationRequirement.REQUIRED)
                            AuthenticatorMakeCredentialRequest.Companion.Options(residentKey = rk, userVerification = uv)
                        }
                        AuthenticatorMakeCredentialRequest(
                            clientDataHash = clientDataHash,
                            rp = options.registerOptions.rp,
                            user = options.registerOptions.user,
                            pubKeyCredParams = options.registerOptions.parameters,
                            excludeList = options.registerOptions.excludeList ?: emptyList(),
                            options = reqOptions,
                        )
                    }

                    RequestOptionsType.SIGN -> {
                        AuthenticatorGetAssertionRequest(
                            rpId = options.signOptions.rpId,
                            clientDataHash = clientDataHash,
                            allowList = options.signOptions.allowList.orEmpty(),
                            options = if (options.signOptions.requireUserVerification == UserVerificationRequirement.REQUIRED) {
                                AuthenticatorGetAssertionRequest.Companion.Options(userVerification = true)
                            } else null
                        )
                    }

                }
                request.let { byteArrayOf(0x01, it.commandByte) + it.encodeParameters() }
            }

            return parseResponse(options, tunnelResp, clientData)
        } catch (e: Throwable) {
            Log.w(TAG, "startHybrid error", e)
            throw e
        } finally {
            hybridClientController.release()
        }
    }

    private fun parseResponse(options: RequestOptions, data: ByteArray, clientData: ByteArray): AuthenticatorResponseWithUser<*> {
        if (data.isEmpty()) error("Empty CTAP data")

        val status = data[0].toInt() and 0xFF
        require(status == 0) { "CTAP error 0x${status.toString(16)}" }

        val cbor = data.copyOfRange(1, data.size)

        return when (options.type) {
            RequestOptionsType.REGISTER -> {
                val result = AuthenticatorMakeCredentialResponse.decodeFromCbor(CBORObject.DecodeFromBytes(cbor))
                val credentialId = AuthenticatorData.decode(result.authData).attestedCredentialData?.id
                AuthenticatorResponseWithUser(
                    AuthenticatorAttestationResponse(
                        credentialId ?: ByteArray(0),
                        clientData,
                        AnyAttestationObject(result.authData, result.fmt, result.attStmt).encode(),
                        arrayOf("cable", "internal")
                    ),
                    options.registerOptions.user
                )
            }
            RequestOptionsType.SIGN -> {
                val result = AuthenticatorGetAssertionResponse.decodeFromCbor(CBORObject.DecodeFromBytes(cbor))
                AuthenticatorResponseWithUser(
                    AuthenticatorAssertionResponse(
                        result.credential!!.id,
                        clientData,
                        result.authData,
                        result.signature,
                        result.user?.id
                    ),
                    result.user
                )
            }
        }
    }
}
