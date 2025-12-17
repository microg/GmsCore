/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.hybrid.controller

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.google.android.gms.fido.fido2.api.common.ErrorCode
import com.upokecenter.cbor.CBORObject
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.microg.gms.fido.core.RequestHandlingException
import org.microg.gms.fido.core.hybrid.CtapError
import org.microg.gms.fido.core.hybrid.HandshakePhase
import org.microg.gms.fido.core.hybrid.ble.HybridBleAdvertiser
import org.microg.gms.fido.core.hybrid.generateEcKeyPair
import org.microg.gms.fido.core.hybrid.hex
import org.microg.gms.fido.core.hybrid.model.QrCodeData
import org.microg.gms.fido.core.hybrid.transport.AuthenticatorTunnelTransport
import org.microg.gms.fido.core.hybrid.transport.TunnelCallback
import org.microg.gms.fido.core.hybrid.tryResumeData
import org.microg.gms.fido.core.hybrid.tryResumeWithError
import org.microg.gms.fido.core.hybrid.tunnel.TunnelException
import org.microg.gms.fido.core.hybrid.tunnel.TunnelWebsocket
import org.microg.gms.fido.core.hybrid.utils.CryptoHelper
import org.microg.gms.fido.core.hybrid.utils.NoiseCrypter
import org.microg.gms.fido.core.hybrid.utils.NoiseHandshakeState
import org.microg.gms.fido.core.protocol.msgs.AuthenticatorGetAssertionRequest
import org.microg.gms.fido.core.protocol.msgs.AuthenticatorGetInfoRequest
import org.microg.gms.fido.core.protocol.msgs.AuthenticatorMakeCredentialRequest
import org.microg.gms.fido.core.protocol.msgs.Ctap2Request
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey

private const val TAG = "AuthenticatorController"

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class HybridAuthenticatorController(context: Context) {
    private val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    private var bleAdvertiser: HybridBleAdvertiser? = null
    private var transport: AuthenticatorTunnelTransport? = null
    private var handshakePhase = HandshakePhase.NONE
    private var noiseState: NoiseHandshakeState? = null
    private var crypter: NoiseCrypter? = null

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    fun release() {
        runCatching { bleAdvertiser?.stopAdvertising() }.onFailure {
            Log.w(TAG, "release: stopAdvertising failed", it)
        }
        bleAdvertiser = null

        runCatching { transport?.stopConnecting() }.onFailure {
            Log.w(TAG, "release: websocket close failed", it)
        }
        transport = null
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    private fun startBleAdvertiser(eidKey: ByteArray, plaintext: ByteArray) {
        bleAdvertiser = bleAdvertiser ?: HybridBleAdvertiser(adapter)
        bleAdvertiser!!.startAdvertising(CryptoHelper.generateEid(eidKey, plaintext))
    }

    suspend fun startAuth(qrCodeData: QrCodeData, handleAuthenticator: suspend (Ctap2Request) -> ByteArray?, completed: (Boolean) -> Unit) = suspendCancellableCoroutine { cont ->
        val randomSeed = qrCodeData.randomSeed
        val peerPublicKey = qrCodeData.peerPublicKey
        val tunnelId = CryptoHelper.endif(ikm = randomSeed, salt = ByteArray(0), info = byteArrayOf(2, 0, 0, 0), length = 16)
        val eidKey = CryptoHelper.endif(ikm = randomSeed, salt = ByteArray(0), info = byteArrayOf(1, 0, 0, 0), length = 64)

        transport = AuthenticatorTunnelTransport(tunnelId, object : TunnelCallback {
            @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
            override fun onSocketConnect(websocket: TunnelWebsocket?, bytes: ByteArray) {
                runCatching {
                    val generatedPlaintext = CryptoHelper.generatedSeed(bytes)
                    startBleAdvertiser(eidKey, generatedPlaintext)
                    noiseState = NoiseHandshakeState(mode = 3).apply {
                        mixHash(byteArrayOf(1))
                        mixHash(CryptoHelper.uncompress(peerPublicKey))
                        mixKeyAndHash(CryptoHelper.endif(ikm = randomSeed, salt = generatedPlaintext, info = byteArrayOf(3, 0, 0, 0), length = 32))
                    }
                    handshakePhase = HandshakePhase.CLIENT_HELLO_SENT
                }.onFailure {
                    if (!cont.isCompleted) cont.tryResumeWithError(it)
                }
            }

            override fun onSocketError(error: TunnelException) {
                if (!cont.isCompleted) cont.tryResumeWithError(RequestHandlingException(ErrorCode.UNKNOWN_ERR, error.message ?: "error"))
            }

            override fun onSocketClose() {
                if (!cont.isCompleted) cont.tryResumeWithError(RequestHandlingException(ErrorCode.UNKNOWN_ERR, "Tunnel closed"))
            }

            @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
            override fun onMessage(websocket: TunnelWebsocket?, data: ByteArray) {
                Log.d(TAG, "Received ${data.size} bytes (phase=$handshakePhase)")
                runBlocking {
                    runCatching {
                        when (handshakePhase) {
                            HandshakePhase.CLIENT_HELLO_SENT -> if (data.size >= 65) handleClientHello(websocket, data, peerPublicKey) else error("Unexpected handshake payload size")
                            HandshakePhase.READY -> handleCtapRequest(websocket, data, handleAuthenticator).also {
                                completed.invoke(it != null)
                                if (!cont.isCompleted) cont.tryResumeData(it)
                            }

                            else -> error("Data received in invalid phase=$handshakePhase")
                        }
                    }.onFailure {
                        if (!cont.isCompleted) cont.tryResumeWithError(it)
                    }
                }
            }
        })

        transport!!.startConnecting()

        cont.invokeOnCancellation { runCatching { transport?.stopConnecting() } }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    private fun handleClientHello(ws: TunnelWebsocket?, data: ByteArray, peerPublicKey: ECPublicKey) {
        bleAdvertiser?.stopAdvertising()
        val state = noiseState ?: error("Noise state not initialized")
        val pcEphemeralPubKey = data.copyOfRange(0, 65)
        val clientHelloPayload = data.copyOfRange(65, data.size)
        state.mixHash(pcEphemeralPubKey)
        state.mixKey(pcEphemeralPubKey)
        val decryptedPayload = state.decryptAndHash(clientHelloPayload)
        if (decryptedPayload.isNotEmpty()) {
            Log.w(TAG, "ClientHello has non-empty payload: ${decryptedPayload.hex()}")
        }
        val ephemeralKeyPair: Pair<ECPublicKey, ECPrivateKey> = generateEcKeyPair()
        val phoneEphemeralPubKey = CryptoHelper.uncompress(ephemeralKeyPair.first)
        state.mixHash(phoneEphemeralPubKey)
        state.mixKey(phoneEphemeralPubKey)
        val peDh = CryptoHelper.recd(ephemeralKeyPair.second, pcEphemeralPubKey)
        state.mixKey(peDh)
        val pcStaticPubKey = CryptoHelper.uncompress(peerPublicKey)
        val psDh = CryptoHelper.recd(ephemeralKeyPair.second, pcStaticPubKey)
        state.mixKey(psDh)
        val serverHelloPayload = state.encryptAndHash(ByteArray(0))
        val serverHello = phoneEphemeralPubKey + serverHelloPayload
        ws?.send(serverHello)

        val (rxKey, txKey) = state.splitSessionKeys()
        crypter = NoiseCrypter(rxKey, txKey)
        handshakePhase = HandshakePhase.READY

        val message = encryptGetInfoPayload() ?: error("Failed to encrypt post-handshake message")

        Log.d(TAG, "Encrypted post-handshake message size: ${message.size} bytes")

        ws?.send(message)
        Log.d(TAG, "✓ Post-handshake message sent successfully")
    }

    private fun encryptGetInfoPayload(): ByteArray? {
        val crypter = this.crypter ?: error("Crypter not initialized, cannot send post-handshake message")

        val payload = AuthenticatorGetInfoRequest(
            versions = listOf("FIDO_2_0", "FIDO_2_1"),
            extensions = listOf("prf"),
            clientDataHash = ByteArray(16),
            options = AuthenticatorGetInfoRequest.Options(residentKey = true, userPresence = true, userVerification = true),
            transports = listOf("internal", "hybrid")
        ).payload

        Log.d(TAG, "GetInfo response size: ${payload.size} bytes")
        Log.d(TAG, "GetInfo response (hex): ${payload.hex()}")

        val getInfoByteString = CBORObject.FromObject(payload)
        val postHandshakeMessage = CBORObject.NewMap().apply {
            set(1, getInfoByteString)
            set(3, CBORObject.NewArray().apply {
                Add("dc")
                Add("ctap")
            })
        }

        val messageBytes = postHandshakeMessage.EncodeToBytes()
        Log.d(TAG, "Post-handshake message size: ${messageBytes.size} bytes")
        Log.d(TAG, "Post-handshake message (hex): ${messageBytes.hex()}")
        return crypter.encrypt(messageBytes)
    }

    private suspend fun handleCtapRequest(ws: TunnelWebsocket?, data: ByteArray, handleAuthenticator: suspend (Ctap2Request) -> ByteArray?): ByteArray? {
        val crypt = this.crypter ?: error("Crypter not initialized (handshake incomplete)")
        val decrypted = crypt.decrypt(data) ?: error("Failed to decrypt CTAP request")
        if (decrypted.isEmpty()) {
            ws?.sendCtapResponse(byteArrayOf(CtapError.INVALID_LENGTH.value))
            return null
        }
        val frameType = decrypted[0].toInt() and 0xFF
        if (frameType == 0x00) {
            Log.d(TAG, "Received post-handshake response from initiator")
            if (decrypted.size > 1) {
                try {
                    val payload = decrypted.copyOfRange(1, decrypted.size)
                    val cbor = CBORObject.DecodeFromBytes(payload)
                    Log.d(TAG, "Post-handshake payload: $cbor")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not parse post-handshake payload (may be empty)", e)
                }
            } else {
                Log.d(TAG, "Post-handshake response has no payload (acknowledgment only)")
            }
            return null
        }
        if (frameType == 0x01) {
            val ctapMessage = decrypted.copyOfRange(1, decrypted.size)
            if (ctapMessage.isEmpty()) {
                ws?.sendCtapResponse(byteArrayOf(CtapError.INVALID_CBOR.value))
                return null
            }
            val params = if (ctapMessage.size > 1) {
                try {
                    val cborBytes = ctapMessage.copyOfRange(1, ctapMessage.size)
                    Log.d(TAG, "CBOR size: ${cborBytes.size} bytes")
                    CBORObject.DecodeFromBytes(cborBytes)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse CBOR parameters", e)
                    null
                }
            } else {
                null
            }
            when (ctapMessage[0]) {
                AuthenticatorMakeCredentialRequest.COMMAND -> {
                    if (params == null) {
                        ws?.sendCtapResponse(byteArrayOf(CtapError.MISSING_PARAMETER.value))
                    } else {
                        val request = AuthenticatorMakeCredentialRequest.decodeFromCbor(params)
                        try {
                            val response = handleAuthenticator(request)
                            if (response == null) {
                                ws?.sendCtapResponse(byteArrayOf(CtapError.OTHER_ERROR.value))
                                return null
                            }
                            ws?.sendCtapResponse(response)
                            return response
                        } catch (e: Exception) {
                            Log.w(TAG, "handleAuthenticatorMakeCredentialRequest: ", e)
                            ws?.sendCtapResponse(byteArrayOf(CtapError.OTHER_ERROR.value))
                        }
                    }
                }

                AuthenticatorGetAssertionRequest.COMMAND -> {
                    if (params == null) {
                        ws?.sendCtapResponse(byteArrayOf(CtapError.MISSING_PARAMETER.value))
                    } else {
                        val request = AuthenticatorGetAssertionRequest.decodeFromCbor(params)
                        try {
                            val response = handleAuthenticator(request)
                            if (response == null) {
                                ws?.sendCtapResponse(byteArrayOf(CtapError.OTHER_ERROR.value))
                                return null
                            }
                            ws?.sendCtapResponse(response)
                            return response
                        } catch (e: Exception) {
                            Log.w(TAG, "handleAuthenticatorGetAssertionRequest: ", e)
                            ws?.sendCtapResponse(byteArrayOf(CtapError.OTHER_ERROR.value))
                        }
                    }
                }

                AuthenticatorGetInfoRequest.COMMAND -> {
                    val payload = AuthenticatorGetInfoRequest(
                        versions = arrayListOf("FIDO_2_0", "FIDO_2_1"),
                        clientDataHash = ByteArray(16),
                        options = AuthenticatorGetInfoRequest.Options(residentKey = true, userPresence = true, userVerification = true)
                    ).payload
                    Log.d(TAG, "GetInfo response: ${payload.size} bytes")
                    val ctapResponse = byteArrayOf(0x00) + payload
                    ws?.sendCtapResponse(ctapResponse)
                }

                else -> {
                    ws?.sendCtapResponse(byteArrayOf(CtapError.INVALID_COMMAND.value))
                }
            }
            return null
        }
        if (frameType == 0x02) {
            Log.d(TAG, "Received UPDATE message")
            if (decrypted.size > 1) {
                val payload = decrypted.copyOfRange(1, decrypted.size)
                Log.d(TAG, "UPDATE payload: ${payload.hex()}")
            }
            return null
        }
        if (frameType == 0x03) {
            Log.d(TAG, "Received JSON message")
            if (decrypted.size > 1) {
                val payload = decrypted.copyOfRange(1, decrypted.size)
                try {
                    val jsonString = String(payload, Charsets.UTF_8)
                    Log.d(TAG, "JSON: $jsonString")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not parse JSON payload", e)
                }
            }
            return null
        }
        return null
    }

    private fun TunnelWebsocket.sendCtapResponse(ctapResponse: ByteArray) {
        try {
            val crypter = crypter ?: error("Crypter not initialized (handshake incomplete)")
            val framedMessage = byteArrayOf(0x01) + ctapResponse
            val encrypted = crypter.encrypt(framedMessage) ?: error("Failed to encrypt CTAP response")
            Log.d(TAG, "Sending encrypted CTAP response: ${encrypted.size} bytes (framed)")
            send(encrypted)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send CTAP response", e)
        }
    }
}