/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.transport.nfc

import android.content.Context
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.fido.core.protocol.msgs.*
import org.microg.gms.fido.core.transport.*
import org.microg.gms.utils.toBase64

class CtapNfcConnection(
    val context: Context,
    val tag: Tag
) : CtapConnection {
    private val isoDep = IsoDep.get(tag)
    override var capabilities: Int = 0

    override suspend fun <Q : Ctap1Request, S : Ctap1Response> runCommand(command: Ctap1Command<Q, S>): S {
        require(hasCtap1Support)
        Log.d(TAG, "Send CTAP1 command: ${command.request.apdu.toBase64(Base64.NO_WRAP)}")
        val (statusCode, payload) = decodeResponseApdu(isoDep.transceive(command.request.apdu))
        Log.d(TAG, "Received CTAP1 response(${(statusCode.toInt() and 0xffff).toString(16)}): ${payload.toBase64(Base64.NO_WRAP)}")
        if (statusCode != 0x9000.toShort()) {
            throw CtapNfcMessageStatusException(statusCode.toInt() and 0xffff)
        }
        return command.decodeResponse(statusCode, payload)
    }

    override suspend fun <Q : Ctap2Request, S : Ctap2Response> runCommand(command: Ctap2Command<Q, S>): S {
        require(hasCtap2Support)
        val request = encodeCommandApdu(0x80.toByte(), 0x10, 0x00, 0x00, byteArrayOf(command.request.commandByte) + command.request.payload, extended = true)
        Log.d(TAG, "Send CTAP2 command: ${request.toBase64(Base64.NO_WRAP)}")
        var (statusCode, payload) = decodeResponseApdu(isoDep.transceive(request))
        Log.d(TAG, "Received CTAP2 response(${(statusCode.toInt() and 0xffff).toString(16)}): ${payload.toBase64(Base64.NO_WRAP)}")
        while (statusCode == 0x9100.toShort()) {
            Log.d(TAG, "Sending GETRESPONSE")
            val res = decodeResponseApdu(isoDep.transceive(encodeCommandApdu(0x00, 0xC0.toByte(), 0x00,0x00)))
            Log.d(TAG, "Received CTAP2 response(${(statusCode.toInt() and 0xffff).toString(16)}): ${payload.toBase64(Base64.NO_WRAP)}")
            statusCode = res.first
            payload = res.second
        }
        if (statusCode != 0x9000.toShort()) {
            throw CtapNfcMessageStatusException(statusCode.toInt() and 0xffff)
        }
        require(payload.isNotEmpty())
        val ctapStatusCode = payload[0]
        if (ctapStatusCode != 0x00.toByte()) {
            throw Ctap2StatusException(ctapStatusCode)
        }
        return command.decodeResponse(payload, 1)
    }

    private fun select(aid: ByteArray): Pair<Short, ByteArray> {
        Log.d(TAG, "Selecting AID: ${aid.toBase64(Base64.NO_WRAP)}")
        return decodeResponseApdu(isoDep.transceive(encodeCommandApdu(0x00, 0xa4.toByte(), 0x04, 0x00, aid)))
    }

    private fun deselect() = isoDep.transceive(encodeCommandApdu(0x80.toByte(), 0x12, 0x01, 0x02))

    private suspend fun fetchCapabilities() {
        val response = runCommand(AuthenticatorGetInfoCommand())
        Log.d(TAG, "Got info: $response")
        capabilities = capabilities or CAPABILITY_CTAP_2 or
                (if (response.versions.contains("FIDO_2_1")) CAPABILITY_CTAP_2_1 else 0) or
                (if (response.options.clientPin == true) CAPABILITY_CLIENT_PIN else 0)
    }

    suspend fun open(): Boolean = withContext(Dispatchers.IO) {
        isoDep.timeout = 5000
        isoDep.connect()
        val (statusCode, version) = select(FIDO2_AID)
        if (statusCode == 0x9000.toShort()) {
            Log.d(TAG, "Device sent version: ${version.decodeToString()}")
            when (version.decodeToString()) {
                "FIDO_2_0" -> {
                    capabilities = CAPABILITY_CTAP_2
                    try {
                        fetchCapabilities()
                    } catch (e: Exception) {
                        Log.w(TAG, e)
                    }
                    true
                }
                "U2F_V2" -> {
                    capabilities = CAPABILITY_CTAP_1 or CAPABILITY_CTAP_2
                    try {
                        fetchCapabilities()
                    } catch (e: Exception) {
                        Log.w(TAG, e)
                        capabilities = CAPABILITY_CTAP_1
                    }
                    true
                }
                else -> {
                    false
                }
            }
        } else {
            false
        }
    }

    suspend fun close() = withContext(Dispatchers.IO) {
        deselect()
        isoDep.close()
        capabilities = 0
    }


    suspend fun <R> open(block: suspend (CtapNfcConnection) -> R): R {
        if (!open()) throw RuntimeException("Could not open device")
        var exception: Throwable? = null
        try {
            return block(this)
        } catch (e: Throwable) {
            exception = e
            throw e
        } finally {
            when (exception) {
                null -> close()
                else -> try {
                    close()
                } catch (closeException: Throwable) {
                    // cause.addSuppressed(closeException) // ignored here
                }
            }
        }
    }

    companion object {
        const val TAG = "FidoCtapNfcConnection"
        private val FIDO2_AID = byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x06, 0x47, 0x2F, 0x00, 0x01)
    }
}

class CtapNfcMessageStatusException(val status: Int) : Exception("Received status ${status.toString(16)}")
