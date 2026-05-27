/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.protocol

import com.google.android.gms.fido.fido2.api.common.Algorithm
import com.google.android.gms.fido.fido2.api.common.EC2Algorithm
import com.google.android.gms.fido.fido2.api.common.RSAAlgorithm
import com.upokecenter.cbor.CBOREncodeOptions
import com.upokecenter.cbor.CBORObject
import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec

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

    fun asCryptoKey(): PublicKey? {
        return when(algorithm) {
            is EC2Algorithm -> {
                val curveName = when (curveId) {
                    1 -> "secp256r1"
                    2 -> "secp384r1"
                    3 -> "secp521r1"
                    4 -> "x25519"
                    5 -> "x448"
                    6 -> "Ed25519"
                    7 -> "Ed448"
                    else -> return null
                }

                val parameters = AlgorithmParameters.getInstance("EC")
                parameters.init(ECGenParameterSpec(curveName))
                val parameterSpec = parameters.getParameterSpec(ECParameterSpec::class.java)
                val keySpec = ECPublicKeySpec(ECPoint(BigInteger(1, x), BigInteger(1, y)), parameterSpec)
                KeyFactory.getInstance("EC").generatePublic(keySpec)
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
