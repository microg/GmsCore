/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.hybrid.utils

import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorResponse
import com.google.android.gms.fido.fido2.api.common.ErrorCode
import com.upokecenter.cbor.CBORObject
import com.upokecenter.cbor.CBORType
import org.microg.gms.fido.core.RequestHandlingException

object CtapProtocol {

    fun parseMakeCredentialResponse(clientDataJson: ByteArray, responseBytes: ByteArray): AuthenticatorResponse {
        val result = Decoder.tryDecodeMake(responseBytes) ?: Decoder.manualFallbackMake(responseBytes)
        return AuthenticatorAttestationResponse(
            result.credentialId ?: ByteArray(0), clientDataJson, result.attestationObject, arrayOf("cable", "internal")
        )
    }

    fun parseGetAssertionResponse(clientDataJson: ByteArray, responseBytes: ByteArray): AuthenticatorResponse {
        val result = Decoder.tryDecodeGet(responseBytes) ?: Decoder.manualFallbackGet(responseBytes)
        return AuthenticatorAssertionResponse(
            result.credentialId, clientDataJson, result.authenticatorData, result.signature, result.userHandle
        )
    }

    private object Decoder {

        fun tryDecodeMake(bytes: ByteArray): CtapResult.Make? {
            return try {
                val map = CBORObject.DecodeFromBytes(bytes)
                if (map.type != CBORType.Map) return null

                val format = map[1]?.AsString() ?: return null
                val authenticatorData = map[2]?.GetByteString() ?: return null
                val attestationStatement = map[3] ?: CBORObject.NewMap()

                val attObj = buildAttestationObject(format, authenticatorData, attestationStatement)
                val credentialId = extractCredentialId(authenticatorData)

                CtapResult.Make(credentialId, attObj)
            } catch (_: Throwable) {
                null
            }
        }

        fun manualFallbackMake(raw: ByteArray): CtapResult.Make {
            val reader = RawReader(raw)

            val mapHeader = reader.readUInt8()
            val entryCount = mapHeader and 0x1f

            var format: String? = null
            var authenticatorData: ByteArray? = null

            repeat(entryCount) {
                val key = reader.readUInt8()
                when (key) {
                    0x01 -> format = reader.readTextString()
                    0x02 -> authenticatorData = reader.readByteString()
                    else -> reader.skipCborField(key)
                }
            }

            val fmt = format ?: error("Missing fmt")
            val auth = authenticatorData ?: error("Missing authData")

            val attObj = buildAttestationObject(fmt, auth)
            val credentialId = extractCredentialId(auth)

            return CtapResult.Make(credentialId, attObj)
        }

        fun tryDecodeGet(bytes: ByteArray): CtapResult.Get? {
            return try {
                val map = CBORObject.DecodeFromBytes(bytes)
                if (map.type != CBORType.Map) return null

                val credentialMap = map[1]
                val credentialId = credentialMap?.getMapItem("id")?.GetByteString() ?: return null

                val authenticatorData = map[2]?.GetByteString() ?: return null
                val signature = map[3]?.GetByteString() ?: return null
                val userHandle = map[4]?.getMapItem("id")?.GetByteString()

                CtapResult.Get(credentialId, authenticatorData, signature, userHandle)
            } catch (_: Throwable) {
                null
            }
        }

        fun manualFallbackGet(raw: ByteArray): CtapResult.Get {
            val reader = RawReader(raw)

            val mapHeader = reader.readUInt8()
            val entryCount = mapHeader and 0x0f

            var credentialId: ByteArray? = null
            var authenticatorData: ByteArray? = null
            var signature: ByteArray? = null
            var userHandle: ByteArray? = null

            repeat(entryCount) {
                when (reader.readUInt8()) {
                    1 -> {
                        val subCount = reader.readUInt8() and 0x0f
                        repeat(subCount) {
                            when (reader.readTextString()) {
                                "id" -> credentialId = reader.readByteString()
                                else -> reader.skipCborNext()
                            }
                        }
                    }

                    2 -> authenticatorData = reader.readByteString()
                    3 -> signature = reader.readByteString()
                    4 -> {
                        val subCount = reader.readUInt8() and 0x0f
                        repeat(subCount) {
                            when (reader.readTextString()) {
                                "id" -> userHandle = reader.readByteString()
                                else -> reader.skipCborNext()
                            }
                        }
                    }

                    else -> reader.skipCborNext()
                }
            }

            if (credentialId == null || authenticatorData == null || signature == null) throw RequestHandlingException(ErrorCode.DATA_ERR, "Missing required fields")

            return CtapResult.Get(credentialId!!, authenticatorData!!, signature!!, userHandle)
        }

        private fun buildAttestationObject(
            format: String, authenticatorData: ByteArray, attestationStatement: CBORObject = CBORObject.NewMap()
        ): ByteArray = CBORObject.NewOrderedMap().apply {
            this["fmt"] = CBORObject.FromObject(format)
            this["attStmt"] = attestationStatement
            this["authData"] = CBORObject.FromObject(authenticatorData)
        }.EncodeToBytes()

        private fun CBORObject.getMapItem(key: String): CBORObject? = if (this.type == CBORType.Map) this[key] else null

        private fun extractCredentialId(authData: ByteArray): ByteArray? {
            if (authData.size < 37) return null

            var offset = 32
            val flags = authData[offset++].toInt()
            offset += 4

            if ((flags and 0x40) == 0) return null
            offset += 16

            val length = ((authData[offset].toInt() and 0xFF) shl 8) or (authData[offset + 1].toInt() and 0xFF)

            offset += 2

            return if (offset + length <= authData.size) authData.copyOfRange(offset, offset + length)
            else null
        }
    }

    private class RawReader(private val source: ByteArray) {
        private var position = 0

        fun readUInt8(): Int = source[position++].toInt() and 0xFF

        fun readBytes(length: Int): ByteArray = source.copyOfRange(position, position + length).also {
            position += length
        }

        fun readByteString(): ByteArray {
            val header = readUInt8()
            return when (header) {
                in 0x40..0x57 -> readBytes(header - 0x40)
                0x58 -> readBytes(readUInt8())
                0x59 -> {
                    val length = (readUInt8() shl 8) or readUInt8()
                    readBytes(length)
                }

                else -> error("Invalid bstr header=0x${header.toString(16)}")
            }
        }

        fun readTextString(): String {
            val header = readUInt8()
            return when (header) {
                in 0x60..0x77 -> String(readBytes(header - 0x60), Charsets.UTF_8)
                0x78 -> String(readBytes(readUInt8()), Charsets.UTF_8)
                else -> error("Invalid tstr header=0x${header.toString(16)}")
            }
        }

        fun skipCborField(header: Int) {
            when (header) {
                in 0x40..0x57 -> readBytes(header - 0x40)
                in 0x60..0x77 -> readBytes(header - 0x60)
                else -> {}
            }
        }

        fun skipCborNext() {
            val h = readUInt8()
            skipCborField(h)
        }
    }

    private sealed interface CtapResult {
        data class Make(val credentialId: ByteArray?, val attestationObject: ByteArray) : CtapResult {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Make

                if (!credentialId.contentEquals(other.credentialId)) return false
                if (!attestationObject.contentEquals(other.attestationObject)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = credentialId?.contentHashCode() ?: 0
                result = 31 * result + attestationObject.contentHashCode()
                return result
            }
        }

        data class Get(
            val credentialId: ByteArray, val authenticatorData: ByteArray, val signature: ByteArray, val userHandle: ByteArray?
        ) : CtapResult {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Get

                if (!credentialId.contentEquals(other.credentialId)) return false
                if (!authenticatorData.contentEquals(other.authenticatorData)) return false
                if (!signature.contentEquals(other.signature)) return false
                if (!userHandle.contentEquals(other.userHandle)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = credentialId.contentHashCode()
                result = 31 * result + authenticatorData.contentHashCode()
                result = 31 * result + signature.contentHashCode()
                result = 31 * result + (userHandle?.contentHashCode() ?: 0)
                return result
            }
        }
    }
}
