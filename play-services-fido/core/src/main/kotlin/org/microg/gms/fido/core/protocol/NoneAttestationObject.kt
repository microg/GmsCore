/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.protocol

import com.upokecenter.cbor.CBORObject

class NoneAttestationObject(authData: AuthenticatorData) : AttestationObject(authData.encode()) {
    override val fmt: String
        get() = "none"
    override val attStmt: CBORObject
        get() = CBORObject.NewMap()
}
