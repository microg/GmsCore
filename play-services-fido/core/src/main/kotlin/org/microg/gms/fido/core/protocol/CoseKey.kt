/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.protocol

import android.os.Build.VERSION.SDK_INT
import androidx.annotation.RequiresApi
import com.google.android.gms.fido.fido2.api.common.Algorithm
import com.google.android.gms.fido.fido2.api.common.EC2Algorithm
import com.upokecenter.cbor.CBOREncodeOptions
import com.upokecenter.cbor.CBORObject
import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.AlgorithmParameterSpec
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.security.spec.NamedParameterSpec

class CoseKey(
    val algorithm: Algorithm,
    val x: ByteArray,
    val y: ByteArray,
    val curveId: Int
) {
    constructor(algorithm: Algorithm, x: BigInteger, y: BigInteger, curveId: Int, curvePointSize: Int) :
            this(algorithm, x.toByteArray(curvePointSize), y.toByteArray(curvePointSize), curveId)

    fun encode(): ByteArray = encodeAsCbor().EncodeToBytes(CBOREncodeOptions.DefaultCtap2Canonical)

    fun encodeAsCbor(): CBORObject = CBORObject.NewMap().apply {
        set(KTY, 2.encodeAsCbor())
        set(ALG, algorithm.algoValue.encodeAsCbor())
        set(CRV, curveId.encodeAsCbor())
        set(X, x.encodeAsCbor())
        set(Y, y.encodeAsCbor())
    }

    sealed class AlgSpec(val keyAlg: String, val agreementAlg: String, val paramSpec: AlgorithmParameterSpec) {
        class EC(algName: String): AlgSpec(
            "EC",
            "ECDH",
            ECGenParameterSpec(algName)
        )
        @RequiresApi(33)
        class XDH(algName: String, private val oid: ByteArray, private val keyLength: Int): AlgSpec(
            algName,
            "XDH",
            NamedParameterSpec(algName)
        ) {
            /**
             * Works for key+preamble smaller than 256 bytes (x25519 is 32 + preamble = 42)
             */
            val x509Preamble: ByteArray
                get() {
                require(keyLength + oid.size + 5 < 0x100)
                val header = byteArrayOf(
                    0x30, oid.size.toByte() // Sequence of OID.size bytes
                ) + oid + byteArrayOf(
                    0x03, (keyLength + 1).toByte(), 0x00 // Bit string of keyLength +1 + 0x00 beginning of the key
                )
                return byteArrayOf(
                    0x30, (header.size + keyLength).toByte(),  // Sequence of header + keylength bytes
                ) + header
            }
        }
    }

    fun getAlgSpec(): AlgSpec? {
        return if (SDK_INT >= 33) {
            // cf. https://www.iana.org/assignments/smi-numbers/smi-numbers.xhtml#smi-numbers-1.3.101
            // for OID
            when (curveId) {
                1 -> AlgSpec.EC("secp256r1")
                2 -> AlgSpec.EC("secp384r1")
                3 -> AlgSpec.EC("secp521r1")
                4 -> AlgSpec.XDH(
                    "x25519",
                    byteArrayOf(0x06, 0x03, 0x2b, 0x65, 0x6e),  // OID: 1.3.101.110 (X25519)
                    32
                )
                5 -> AlgSpec.XDH(
                    "x448",
                    byteArrayOf(0x06, 0x03, 0x2b, 0x65, 0x6f),  // OID: 1.3.101.111 (X448)
                    56
                )
                6 -> AlgSpec.XDH(
                    "Ed25519",
                    byteArrayOf(0x06, 0x03, 0x2b, 0x65, 0x70),  // OID: 1.3.101.112 (ED25519)
                    32
                )
                7 -> AlgSpec.XDH(
                    "Ed448",
                    byteArrayOf(0x06, 0x03, 0x2b, 0x65, 0x77),  // OID: 1.3.101.113 (ED448)
                    56
                )
                else -> null
            }
        } else {
            when (curveId) {
                1 -> AlgSpec.EC("secp256r1")
                2 -> AlgSpec.EC("secp384r1")
                3 -> AlgSpec.EC("secp521r1")
                else -> null
            }
        }
    }

    fun asCryptoKey(): PublicKey? {
        return when(algorithm) {
            is EC2Algorithm -> {
                val algSpec = getAlgSpec() ?: return null
                if (algSpec is AlgSpec.EC) {
                    val parameters = AlgorithmParameters.getInstance("EC")
                    parameters.init(algSpec.paramSpec)
                    val parameterSpec = parameters.getParameterSpec(ECParameterSpec::class.java)
                    val keySpec = ECPublicKeySpec(ECPoint(BigInteger(1, x), BigInteger(1, y)), parameterSpec)
                    KeyFactory.getInstance("EC").generatePublic(keySpec)
                } else if (SDK_INT >= 33 && algSpec is AlgSpec.XDH) {
                    object : PublicKey {
                        override fun getAlgorithm(): String = algSpec.keyAlg
                        override fun getFormat(): String = "x.509"
                        override fun getEncoded(): ByteArray = algSpec.x509Preamble + x
                    }
                } else {
                    null
                }
            }
            else -> null
        }
    }

    companion object {
        const val KTY = 1
        const val ALG = 3
        const val CRV = -1
        const val X = -2
        const val Y = -3

        fun decode(bytes: ByteArray): CoseKey = decodeFromCbor(CBORObject.DecodeFromBytes(bytes))

        fun decodeFromCbor(obj: CBORObject): CoseKey = CoseKey(
            getAlgorithm(obj.get(CoseKey.ALG).AsInt32Value()),
            obj.get(CoseKey.X).GetByteString(),
            obj.get(CoseKey.Y).GetByteString(),
            obj.get(CoseKey.CRV).AsInt32Value()
        )

        fun BigInteger.toByteArray(size: Int): ByteArray {
            val res = ByteArray(size)
            val orig = toByteArray()
            if (orig.size > size) {
                System.arraycopy(orig, orig.size - size, res, 0, size)
            } else {
                System.arraycopy(orig, 0, res, size - orig.size, orig.size)
            }
            return res
        }
    }
}
