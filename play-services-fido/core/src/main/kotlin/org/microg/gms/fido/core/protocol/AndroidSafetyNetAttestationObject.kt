/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.protocol

import com.upokecenter.cbor.CBORObject

class AndroidSafetyNetAttestationObject(authData: AuthenticatorData, val ver: String, val response: ByteArray) :
    AttestationObject(authData.encode()) {
    override val fmt: String
        get() = "android-safetynet"
    override val attStmt: CBORObject
        get() = CBORObject.NewMap().apply {
            set("ver", ver.encodeAsCbor())
            set("response", response.encodeAsCbor())
        }
}
