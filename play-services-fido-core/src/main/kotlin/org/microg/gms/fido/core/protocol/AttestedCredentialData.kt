/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.protocol

import com.upokecenter.cbor.CBORObject
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AttestedCredentialData(val aaguid: ByteArray, val id: ByteArray, val publicKey: ByteArray) {
    fun encode() = ByteBuffer.allocate(aaguid.size + 2 + id.size + publicKey.size)
        .put(aaguid)
        .order(ByteOrder.BIG_ENDIAN).putShort(id.size.toShort())
        .put(id)
        .put(publicKey)
        .array()

    companion object {
        fun decode(buffer: ByteBuffer) = buffer.run {
            val aaguid = ByteArray(16)
            get(aaguid)
            val idSize = order(ByteOrder.BIG_ENDIAN).short.toInt() and 0xffff
            val id = ByteArray(idSize)
            get(id)
            mark()
            val remaining = ByteArray(remaining())
            get(remaining)
            val bis = ByteArrayInputStream(remaining)
            CBORObject.Read(bis) // Read object and ignore, we only want to know the size
            reset()
            val publicKey = ByteArray(remaining() - bis.available())
            get(publicKey)
            return@run AttestedCredentialData(aaguid, id, publicKey)
        }
    }
}
