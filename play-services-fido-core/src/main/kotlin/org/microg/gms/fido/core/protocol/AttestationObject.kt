/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.protocol

import com.upokecenter.cbor.CBORObject

abstract class AttestationObject(val authData: AuthenticatorData) {
    abstract val fmt: String
    abstract val attStmt: CBORObject

    fun encode(): ByteArray = CBORObject.NewMap().apply {
        set("fmt", fmt.encodeAsCbor())
        set("attStmt", attStmt)
        set("authData", authData.encodeAsCbor())
    }.EncodeToBytes()
}
