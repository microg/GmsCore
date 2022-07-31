/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.protocol

import com.google.android.gms.fido.fido2.api.common.Algorithm
import com.upokecenter.cbor.CBORObject
import java.math.BigInteger

class CoseKey(
    val algorithm: Algorithm,
    val x: BigInteger,
    val y: BigInteger,
    val curveId: Int,
    val curvePointSize: Int
) {
    fun encode(): ByteArray = CBORObject.NewMap().apply {
        set(1, 2.encodeAsCbor())
        set(3, algorithm.algoValue.encodeAsCbor())
        set(-1, curveId.encodeAsCbor())
        set(-2, x.toByteArray(curvePointSize).encodeAsCbor())
        set(-3, y.toByteArray(curvePointSize).encodeAsCbor())
    }.EncodeToBytes()

    companion object {
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
