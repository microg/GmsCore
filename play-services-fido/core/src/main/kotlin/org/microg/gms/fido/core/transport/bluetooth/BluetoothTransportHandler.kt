/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.transport.bluetooth

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import com.google.android.gms.fido.fido2.api.common.AuthenticatorResponse
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions
import com.google.android.gms.fido.fido2.api.common.RequestOptions
import com.google.android.gms.fido.fido2.api.common.UserVerificationRequirement
import org.microg.gms.fido.core.getClientDataAndHash
import org.microg.gms.fido.core.hybrid.controller.HybridClientController
import org.microg.gms.fido.core.hybrid.generateEcKeyPair
import org.microg.gms.fido.core.hybrid.model.QrCodeData
import org.microg.gms.fido.core.hybrid.utils.CtapProtocol
import org.microg.gms.fido.core.protocol.msgs.AuthenticatorGetAssertionRequest
import org.microg.gms.fido.core.protocol.msgs.AuthenticatorMakeCredentialRequest
import org.microg.gms.fido.core.transport.Transport
import org.microg.gms.fido.core.transport.TransportHandler
import org.microg.gms.fido.core.transport.TransportHandlerCallback

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class BluetoothTransportHandler(private val context: Context, callback: TransportHandlerCallback? = null) : TransportHandler(Transport.BLUETOOTH, callback) {
    override val isSupported: Boolean
        get() = context.getSystemService<BluetoothManager>()?.adapter != null

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override suspend fun start(
        options: RequestOptions, callerPackage: String, pinRequested: Boolean, pin: String?, userInfo: String?
    ): AuthenticatorResponse {
        val staticKey = generateEcKeyPair()
        val hybridClientController = HybridClientController(context, staticKey)
        try {
            callback?.onStatusChanged(
                Transport.BLUETOOTH, "QR_CODE_READY", bundleOf("qrCodeBitmap" to QrCodeData.generateQrCode(staticKey.first, options.challenge))
            )
            val eid = hybridClientController.startBluetoothScan()
            callback?.onStatusChanged(Transport.BLUETOOTH, "CONNECTING", null)

            val (clientData, clientDataHash) = getClientDataAndHash(context, options, callerPackage)
            val tunnelResp = hybridClientController.startClientTunnel(eid, options.challenge) {
                Log.d(TAG, "start: options: $options")
                when (options) {
                    is PublicKeyCredentialCreationOptions -> {
                        val reqOptions = options.authenticatorSelection?.let {
                            val rk = (it.requireResidentKey == true || it.residentKeyRequirement?.toString() == UserVerificationRequirement.REQUIRED.name)
                            val uv = (it.requireUserVerification == UserVerificationRequirement.REQUIRED)
                            AuthenticatorMakeCredentialRequest.Companion.Options(rk, uv)
                        }
                        AuthenticatorMakeCredentialRequest(
                            clientDataHash = clientDataHash,
                            rp = options.rp,
                            user = options.user,
                            pubKeyCredParams = options.parameters,
                            excludeList = options.excludeList ?: emptyList(),
                            options = reqOptions,
                        ).encode()
                    }

                    is PublicKeyCredentialRequestOptions -> {
                        AuthenticatorGetAssertionRequest(
                            rpId = options.rpId,
                            clientDataHash = clientDataHash,
                            allowList = options.allowList.orEmpty(),
                            options = if (options.requireUserVerification == UserVerificationRequirement.REQUIRED) {
                                AuthenticatorGetAssertionRequest.Companion.Options(userVerification = true)
                            } else null
                        ).encode()
                    }

                    else -> null
                }
            }

            return parseResponse(options, tunnelResp, clientData)
        } catch (e: Throwable) {
            Log.w(TAG, "startHybrid error", e)
            throw e
        } finally {
            hybridClientController.release()
        }
    }

    private fun parseResponse(options: RequestOptions, data: ByteArray, clientData: ByteArray): AuthenticatorResponse {
        if (data.isEmpty()) error("Empty CTAP data")

        val status = data[0].toInt() and 0xFF
        require(status == 0) { "CTAP error 0x${status.toString(16)}" }

        val cbor = data.copyOfRange(1, data.size)

        return when (options) {
            is PublicKeyCredentialCreationOptions -> CtapProtocol.parseMakeCredentialResponse(clientData, cbor)
            is PublicKeyCredentialRequestOptions -> CtapProtocol.parseGetAssertionResponse(clientData, cbor)
            else -> error("Unknown RequestOptions")
        }
    }
}
