/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.protocol.msgs

fun encodeCommandApdu(
    cla: Byte,
    ins: Byte,
    p1: Byte,
    p2: Byte,
    data: ByteArray = ByteArray(0),
    le: Int = -1,
    extended: Boolean = data.size > 255 || le > 255
): ByteArray {
    val fixed = byteArrayOf(cla, ins, p1, p2)
    val ext = if (extended) byteArrayOf(0) else ByteArray(0)
    val req = when {
        data.isEmpty() -> ByteArray(0)
        extended -> byteArrayOf((data.size shr 8).toByte(), data.size.toByte()) + data
        else -> byteArrayOf(data.size.toByte()) + data
    }
    val res = when {
        le == -1 -> ByteArray(0)
        extended -> byteArrayOf((le shr 8).toByte(), le.toByte())
        else -> byteArrayOf(le.toByte())
    }
    return fixed + ext + req + res
}

fun decodeResponseApdu(bytes: ByteArray): Pair<Short, ByteArray> {
    require(bytes.size >= 2)
    return ((bytes[bytes.size - 2].toInt() and 0xff shl 8) + (bytes.last()
        .toInt() and 0xff)).toShort() to bytes.sliceArray(0 until bytes.size - 2)
}
