/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.protocol

import org.microg.gms.fido.core.digest
import java.nio.ByteBuffer
import java.security.PublicKey

class CredentialId(val type: Byte, val data: ByteArray, val rpId: String, val publicKey: PublicKey) {
    fun encode(): ByteArray = ByteBuffer.allocate(1 + data.size + 32).apply {
        put(type)
        put(data)
        put((rpId.toByteArray() + publicKey.encoded).digest("SHA-256"))
    }.array()

    companion object {
        fun decodeTypeAndData(bytes: ByteArray): Pair<Byte, ByteArray> {
            val buffer = ByteBuffer.wrap(bytes)
            val type = buffer.get()
            val data = ByteArray(32)
            buffer.get(data)
            return type to data
        }
    }
}
