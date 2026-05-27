/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.protocol

import com.upokecenter.cbor.CBOREncodeOptions
import com.upokecenter.cbor.CBORObject

abstract class AttestationObject(val authData: ByteArray) {
    abstract val fmt: String
    abstract val attStmt: CBORObject

    fun encode(): ByteArray = CBORObject.NewMap().apply {
        set("fmt", fmt.encodeAsCbor())
        set("attStmt", attStmt)
        set("authData", authData.encodeAsCbor())
    }.EncodeToBytes(CBOREncodeOptions.DefaultCtap2Canonical)

    companion object {
        fun decode(bytes: ByteArray): AttestationObject = decodeFromCbor(CBORObject.DecodeFromBytes(bytes))

        fun decodeFromCbor(obj: CBORObject): AttestationObject {
            return AnyAttestationObject(
                authData = obj["authData"].GetByteString(),
                fmt = obj["fmt"]?.AsString() ?: "none",
                attStmt = obj["attStmt"]
            )
        }
    }
}

class AnyAttestationObject(authData: ByteArray, override val fmt: String, override val attStmt: CBORObject) : AttestationObject(authData)
