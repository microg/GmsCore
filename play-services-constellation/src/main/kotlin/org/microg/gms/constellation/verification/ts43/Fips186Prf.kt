package org.microg.gms.constellation.verification.ts43

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object Fips186Prf {
    fun deriveKeys(
        identityBytes: ByteArray,
        ik: ByteArray,
        ck: ByteArray
    ): Map<String, ByteArray> {
        val xKey = try {
            val md = MessageDigest.getInstance("SHA-1")
            md.update(identityBytes)
            md.update(ik)
            md.update(ck)
            md.digest()
        } catch (_: NoSuchAlgorithmException) {
            ByteArray(20)
        }

        if (xKey.size != 20) return emptyMap()

        val result = ByteArray(160)
        val xKeyWorking = xKey.copyOf()
        var resultOffset = 0

        repeat(8) {
            val h = intArrayOf(
                0x67452301,
                0xEFCDAB89.toInt(),
                0x98BADCFE.toInt(),
                0x10325476,
                0xC3D2E1F0.toInt()
            )
            val w = IntArray(80)
            for (k in 0 until 16) {
                val wordIdx = k * 4
                w[k] = if (wordIdx < 20) {
                    ((xKeyWorking.getOrElse(wordIdx) { 0 }.toInt() and 0xFF) shl 24) or
                            ((xKeyWorking.getOrElse(wordIdx + 1) { 0 }
                                .toInt() and 0xFF) shl 16) or
                            ((xKeyWorking.getOrElse(wordIdx + 2) { 0 }
                                .toInt() and 0xFF) shl 8) or
                            (xKeyWorking.getOrElse(wordIdx + 3) { 0 }.toInt() and 0xFF)
                } else 0
            }
            for (k in 16 until 80) {
                val temp = w[k - 3] xor w[k - 8] xor w[k - 14] xor w[k - 16]
                w[k] = (temp shl 1) or (temp ushr 31)
            }
            var a = h[0]
            var b = h[1]
            var c = h[2]
            var d = h[3]
            var e = h[4]
            for (t in 0 until 80) {
                val f: Int
                val k: Int
                when {
                    t <= 19 -> {
                        f = (b and c) or (b.inv() and d); k = 0x5A827999
                    }

                    t <= 39 -> {
                        f = b xor c xor d; k = 0x6ED9EBA1
                    }

                    t <= 59 -> {
                        f = (b and c) or (b and d) or (c and d); k = 0x8F1BBCDC.toInt()
                    }

                    else -> {
                        f = b xor c xor d; k = 0xCA62C1D6.toInt()
                    }
                }
                val temp = ((a shl 5) or (a ushr 27)) + f + e + k + w[t]
                e = d; d = c; c = (b shl 30) or (b ushr 2); b = a; a = temp
            }
            val block = IntArray(5)
            block[0] = h[0] + a; block[1] = h[1] + b; block[2] = h[2] + c; block[3] =
            h[3] + d; block[4] = h[4] + e
            for (k in 0 until 5) {
                val word = block[k]
                result[resultOffset++] = (word shr 24).toByte()
                result[resultOffset++] = (word shr 16).toByte()
                result[resultOffset++] = (word shr 8).toByte()
                result[resultOffset++] = word.toByte()
            }
            var carry = 1
            for (k in 19 downTo 0) {
                val resByte = result[resultOffset - 20 + k].toInt() and 0xFF
                val keyByte = xKeyWorking[k].toInt() and 0xFF
                val sum = carry + keyByte + resByte
                xKeyWorking[k] = sum.toByte()
                carry = sum shr 8
            }
        }

        // RFC 4187 Section 7: PRF output slicing
        return mapOf(
            "K_encr" to result.copyOfRange(0, 16),
            "K_aut" to result.copyOfRange(16, 32),
            "MSK" to result.copyOfRange(32, 96),
            "EMSK" to result.copyOfRange(96, 160)
        )
    }
}