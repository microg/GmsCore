/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.protocol

import com.google.android.gms.fido.fido2.api.common.Algorithm
import com.upokecenter.cbor.CBORObject

class AndroidKeyAttestationObject(
    authData: AuthenticatorData,
    val alg: Algorithm,
    val sig: ByteArray,
    val x5c: List<ByteArray>
) :
    AttestationObject(authData.encode()) {
    override val fmt: String
        get() = "android-key"
    override val attStmt: CBORObject
        get() = CBORObject.NewMap().apply {
            set("alg", alg.algoValue.encodeAsCbor())
            set("sig", sig.encodeAsCbor())
            set("x5c", CBORObject.NewArray().apply {
                for (certificate in x5c) {
                    Add(certificate.encodeAsCbor())
                }
            })
        }
}
