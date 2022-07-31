/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.protocol

import com.upokecenter.cbor.CBORObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.experimental.or

class AuthenticatorData(
    val rpIdHash: ByteArray,
    val userPresent: Boolean,
    val userVerified: Boolean,
    val signCount: Int,
    val attestedCredentialData: AttestedCredentialData? = null,
    val extensions: ByteArray? = null
) {
    fun encode(): ByteArray {
        val attestedCredentialData = attestedCredentialData?.encode() ?: ByteArray(0)
        val extensions = extensions ?: ByteArray(0)
        return ByteBuffer.allocate(rpIdHash.size + 5 + attestedCredentialData.size + extensions.size)
            .put(rpIdHash)
            .put(buildFlags(userPresent, userVerified, attestedCredentialData.isNotEmpty(), extensions.isNotEmpty()))
            .order(ByteOrder.BIG_ENDIAN).putInt(signCount)
            .put(attestedCredentialData)
            .put(extensions)
            .array()
    }

    fun toCBOR(): CBORObject = encode().encodeAsCbor()

    companion object {
        /** User Present **/
        private const val FLAG_UP: Byte = 1

        /** User Verified **/
        private const val FLAG_UV: Byte = 4

        /** Attested credential data included **/
        private const val FLAG_AT: Byte = 64

        /** Extension data included **/
        private const val FLAG_ED: Byte = -128

        private fun buildFlags(up: Boolean, uv: Boolean, at: Boolean, ed: Boolean): Byte =
            (if (up) FLAG_UP else 0) or (if (uv) FLAG_UV else 0) or (if (at) FLAG_AT else 0) or (if (ed) FLAG_ED else 0)
    }
}
