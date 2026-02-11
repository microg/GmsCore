/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.hybrid.model

import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.upokecenter.cbor.CBORObject
import com.upokecenter.cbor.CBORType
import org.microg.gms.fido.core.hybrid.EC_ALGORITHM
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec

private const val TAG = "HybridQrCodeData"

data class QrCodeData(
    val peerPublicKey: ECPublicKey,  // PC's static public key
    val randomSeed: ByteArray,       // 16-byte random seed (IKM for key derivation)
    val version: Int,                // Protocol version
    val timestamp: Long,             // Timestamp in seconds
    val isLinkingFlow: Boolean,      // Whether this is a linking flow
    val flowIdentifier: String?      // Optional flow type identifier
) {
    companion object {
        const val PREFIX_FIDO = "FIDO:/"
        private val PADDING_TABLE = intArrayOf(0, 3, 5, 8, 10, 13, 15)

        fun parse(data: String): QrCodeData? {
            val encoded = data.substringAfter(PREFIX_FIDO, "")
            Log.d(TAG, "encoded: $encoded")
            val qrCodeDataByte = resolveQrCodeData(encoded)
            Log.d(TAG, "qrCodeDataByte: $qrCodeDataByte")
            return qrCodeDataByte?.let {
                val cbor = CBORObject.DecodeFromBytes(it)
                if (cbor.type != CBORType.Map) return null

                val publicKeyBytes = cbor[0]?.GetByteString() ?: return null
                val randomSeed = cbor[1]?.GetByteString() ?: return null
                val publicKey = decompressECPublicKey(publicKeyBytes) ?: return null

                QrCodeData(
                    peerPublicKey = publicKey,
                    randomSeed = randomSeed,
                    version = cbor[2]?.AsInt32() ?: 0,
                    timestamp = cbor[3]?.AsInt64() ?: 0L,
                    isLinkingFlow = cbor[4]?.AsBoolean() ?: false,
                    flowIdentifier = cbor[5]?.AsString()
                )
            }
        }

        fun generateQrCode(staticPublicKey: ECPublicKey, randomSeed: ByteArray): Bitmap {
            val content = buildQrCborPayload(staticPublicKey, randomSeed)
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 512, 512)
            return createBitmap(matrix.width, matrix.height, Bitmap.Config.RGB_565).also { bmp ->
                for (x in 0 until matrix.width) {
                    for (y in 0 until matrix.height) {
                        bmp[x, y] = if (matrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                    }
                }
            }
        }

        private fun buildQrCborPayload(staticPublicKey: ECPublicKey, randomSeed16: ByteArray): String {
            val cbor = CBORObject.NewOrderedMap().apply {
                compressECPublicKey(staticPublicKey).let {
                    this[0] = CBORObject.FromObject(it)
                }
                val secret = randomSeed16.takeIf { it.isNotEmpty() } ?: ByteArray(16).also { SecureRandom().nextBytes(it) }
                this[1] = CBORObject.FromObject(secret)
                this[2] = CBORObject.FromObject(2)
                this[3] = CBORObject.FromObject(System.currentTimeMillis() / 1000L)
                this[4] = CBORObject.False
            }
            val bytes = cbor.EncodeToBytes()
            val gmsBase34 = encodeGmsBase34(bytes)
            return "$PREFIX_FIDO$gmsBase34"
        }

        private fun encodeGmsBase34(data: ByteArray): String {
            val sb = StringBuilder((data.size / 7 + 1) * 17)
            var v = 0L
            var i = 0
            var rem = 0
            while (i < data.size) {
                v = v or ((data[i].toLong() and 0xFFL) shl (rem * 8))
                i++
                rem = i % 7
                if (rem == 0) {
                    val s = v.toString()
                    if (s.length < 17) sb.append("0".repeat(17 - s.length))
                    sb.append(s)
                    v = 0
                }
            }
            if (rem != 0) {
                val s = v.toString()
                val padTable = intArrayOf(0, 3, 5, 8, 10, 13, 15)
                val need = padTable[rem] - s.length
                if (need > 0) sb.append("0".repeat(need))
                sb.append(s)
            }
            return sb.toString()
        }

        private fun compressECPublicKey(pub: ECPublicKey): ByteArray {
            val p = pub.w
            val x = p.affineX.toByteArray().takeLast(32).toByteArray()
            val y = p.affineY.toByteArray().takeLast(32).toByteArray()
            val prefix = if ((y.last().toInt() and 1) == 0) 0x02 else 0x03
            return byteArrayOf(prefix.toByte()) + x
        }

        private fun decompressECPublicKey(compressed: ByteArray): ECPublicKey? {
            try {
                if (compressed.size != 33) {
                    Log.e(TAG, "Invalid compressed key size: ${compressed.size}")
                    return null
                }

                val prefix = compressed[0].toInt()
                if (prefix != 0x02 && prefix != 0x03) {
                    Log.e(TAG, "Invalid compressed key prefix: $prefix")
                    return null
                }

                // Extract x-coordinate
                val xBytes = compressed.copyOfRange(1, 33)
                val x = BigInteger(1, xBytes)

                // Recover y-coordinate using curve equation: y² = x³ - 3x + b (mod p)
                val spec = java.security.spec.ECGenParameterSpec("secp256r1")
                val kpg = KeyPairGenerator.getInstance(EC_ALGORITHM)
                kpg.initialize(spec)
                val params = (kpg.generateKeyPair().public as ECPublicKey).params

                val p = params.curve.field.fieldSize.let {
                    BigInteger("FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFF", 16)
                }
                val b = params.curve.b

                // Calculate y² = x³ - 3x + b
                val x3 = x.modPow(BigInteger.valueOf(3), p)
                val ax = x.multiply(BigInteger.valueOf(3)).mod(p)
                val ySquared = x3.subtract(ax).add(b).mod(p)

                // Calculate y = sqrt(y²) mod p using Tonelli-Shanks
                val y = modSqrt(ySquared, p) ?: run {
                    Log.e(TAG, "Failed to calculate square root")
                    return null
                }

                // Choose correct y based on prefix (even/odd)
                val yFinal = if ((y.testBit(0) && prefix == 0x03) || (!y.testBit(0) && prefix == 0x02)) {
                    y
                } else {
                    p.subtract(y)
                }

                // Create ECPublicKey
                val point = ECPoint(x, yFinal)
                val keySpec = ECPublicKeySpec(point, params)
                val keyFactory = KeyFactory.getInstance(EC_ALGORITHM)
                return keyFactory.generatePublic(keySpec) as ECPublicKey
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decompress EC public key", e)
                return null
            }
        }

        private fun modSqrt(n: BigInteger, p: BigInteger): BigInteger? {
            // For p ≡ 3 (mod 4), sqrt(n) = n^((p+1)/4) mod p
            val exponent = p.add(BigInteger.ONE).divide(BigInteger.valueOf(4))
            val result = n.modPow(exponent, p)

            // Verify result
            return if (result.modPow(BigInteger.valueOf(2), p) == n.mod(p)) {
                result
            } else {
                null
            }
        }

        private fun resolveQrCodeData(encoded: String): ByteArray? {
            try {
                val length = encoded.length
                val fullBlocks = length / 17
                val remainder = length % 17

                // Validate remainder using padding table
                val remainingBytes = PADDING_TABLE.indexOfFirst { length % 17 == it }
                if (remainingBytes == -1) {
                    Log.e(TAG, "Invalid Base17 length: $length (remainder: $remainder)")
                    return null
                }

                val totalBytes = fullBlocks * 7 + remainingBytes
                val result = ByteArray(totalBytes)
                val buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)

                // Decode full blocks (17 digits → 7 bytes)
                for (i in 0 until fullBlocks) {
                    val digitGroup = encoded.substring(i * 17, (i + 1) * 17)
                    val longValue = digitGroup.toLongOrNull() ?: throw IllegalArgumentException("Invalid digit group: $digitGroup")

                    buffer.rewind()
                    buffer.putLong(longValue)
                    buffer.rewind()
                    buffer.get(result, i * 7, 7)

                    // Verify high byte is 0
                    if (buffer.get() != 0.toByte()) {
                        throw IllegalArgumentException("Decoded long does not fit in 7 bytes")
                    }
                }

                // Decode remaining digits
                if (remainder > 0) {
                    val remainingDigits = encoded.substring(fullBlocks * 17)
                    val longValue = remainingDigits.toLongOrNull() ?: throw IllegalArgumentException("Invalid remaining digits: $remainingDigits")

                    buffer.rewind()
                    buffer.putLong(longValue)
                    buffer.rewind()
                    buffer.get(result, totalBytes - remainingBytes, remainingBytes)

                    // Verify remaining bytes are 0
                    while (buffer.hasRemaining()) {
                        if (buffer.get() != 0.toByte()) {
                            throw IllegalArgumentException("Decoded long does not fit in remaining bytes")
                        }
                    }
                }

                return result
            } catch (e: Exception) {
                Log.e(TAG, "Base17 decoding failed", e)
                return null
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as QrCodeData
        return peerPublicKey == other.peerPublicKey && randomSeed.contentEquals(other.randomSeed) && version == other.version && timestamp == other.timestamp && isLinkingFlow == other.isLinkingFlow && flowIdentifier == other.flowIdentifier
    }

    override fun hashCode(): Int {
        var result = peerPublicKey.hashCode()
        result = 31 * result + randomSeed.contentHashCode()
        result = 31 * result + version
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + isLinkingFlow.hashCode()
        result = 31 * result + (flowIdentifier?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "QrCodeData(version=$version, timestamp=$timestamp, " + "isLinking=$isLinkingFlow, flow=$flowIdentifier, " + "seedSize=${randomSeed.size})"
    }
}