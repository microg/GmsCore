/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.transport

import com.google.android.gms.fido.fido2.api.common.ErrorCode
import org.microg.gms.fido.core.RequestHandlingException
import org.microg.gms.fido.core.protocol.msgs.*

interface CtapConnection {
    val hasCtap1Support: Boolean
    val hasCtap2Support: Boolean

    suspend fun <Q : Ctap1Request, S : Ctap1Response> runCommand(command: Ctap1Command<Q, S>): S
    suspend fun <Q : Ctap2Request, S : Ctap2Response> runCommand(command: Ctap2Command<Q, S>): S
}
