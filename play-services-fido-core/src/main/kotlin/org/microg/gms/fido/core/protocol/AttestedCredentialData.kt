/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder

class AttestedCredentialData(val aaguid: ByteArray, val id: ByteArray, val publicKey: ByteArray) {
    fun encode() = ByteBuffer.allocate(aaguid.size + 2 + id.size + publicKey.size)
        .put(aaguid)
        .order(ByteOrder.BIG_ENDIAN).putShort(id.size.toShort())
        .put(id)
        .put(publicKey)
        .array()
}
