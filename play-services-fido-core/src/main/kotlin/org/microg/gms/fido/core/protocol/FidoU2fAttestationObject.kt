/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.protocol

import com.upokecenter.cbor.CBORObject

class FidoU2fAttestationObject(authData: AuthenticatorData, val signature: ByteArray, val attestationCertificate: ByteArray) :
    AttestationObject(authData.encode()) {
    override val fmt: String
        get() = "fido-u2f"
    override val attStmt: CBORObject
        get() = CBORObject.NewMap().apply {
            set("sig", signature.encodeAsCbor())
            set("x5c", CBORObject.NewArray().apply { Add(attestationCertificate.encodeAsCbor()) })
        }
}
