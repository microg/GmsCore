/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.protocol

import com.google.android.gms.fido.fido2.api.common.Algorithm
import com.upokecenter.cbor.CBOREncodeOptions
import com.upokecenter.cbor.CBORObject
import java.math.BigInteger

class CoseKey(
    val algorithm: Algorithm,
    val x: ByteArray,
    val y: ByteArray,
    val curveId: Int
) {
    constructor(algorithm: Algorithm, x: BigInteger, y: BigInteger, curveId: Int, curvePointSize: Int) :
            this(algorithm, x.toByteArray(curvePointSize), y.toByteArray(curvePointSize), curveId)

    fun encode(): ByteArray = CBORObject.NewMap().apply {
        set(KTY, 2.encodeAsCbor())
        set(ALG, algorithm.algoValue.encodeAsCbor())
        set(CRV, curveId.encodeAsCbor())
        set(X, x.encodeAsCbor())
        set(Y, y.encodeAsCbor())
    }.EncodeToBytes(CBOREncodeOptions.DefaultCtap2Canonical)

    companion object {
        private const val KTY = 1
        private const val ALG = 3
        private const val CRV = -1
        private const val X = -2
        private const val Y = -3

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
