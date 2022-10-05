/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.transport

import com.google.android.gms.fido.fido2.api.common.ErrorCode
import org.microg.gms.fido.core.RequestHandlingException
import org.microg.gms.fido.core.protocol.msgs.*

const val CAPABILITY_CTAP_1 = 1 shl 0
const val CAPABILITY_CTAP_2 = 1 shl 1
const val CAPABILITY_CTAP_2_1 = 1 shl 2
const val CAPABILITY_CLIENT_PIN = 1 shl 3
const val CAPABILITY_WINK = 1 shl 4
const val CAPABILITY_MAKE_CRED_WITHOUT_UV = 1 shl 5

interface CtapConnection {
    val capabilities: Int

    val hasCtap1Support: Boolean
        get() = capabilities and CAPABILITY_CTAP_1 > 0
    val hasCtap2Support: Boolean
        get() = capabilities and CAPABILITY_CTAP_2 > 0
    val hasCtap21Support: Boolean
        get() = capabilities and CAPABILITY_CTAP_2_1 > 0
    val hasClientPin: Boolean
        get() = capabilities and CAPABILITY_CLIENT_PIN > 0
    val hasWinkSupport: Boolean
        get() = capabilities and CAPABILITY_WINK > 0
    val canMakeCredentialWithoutUserVerification: Boolean
        get() = capabilities and CAPABILITY_MAKE_CRED_WITHOUT_UV > 0

    suspend fun <Q : Ctap1Request, S : Ctap1Response> runCommand(command: Ctap1Command<Q, S>): S
    suspend fun <Q : Ctap2Request, S : Ctap2Response> runCommand(command: Ctap2Command<Q, S>): S
}

class Ctap2StatusException(val status: Byte) : Exception("Received status ${(status.toInt() and 0xff).toString(16)}")
