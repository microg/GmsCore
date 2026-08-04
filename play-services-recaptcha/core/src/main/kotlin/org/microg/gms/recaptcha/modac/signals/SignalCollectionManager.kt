/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.recaptcha.modac.signals

import org.microg.gms.recaptcha.qr.CachedInitCredential
import org.microg.gms.recaptcha.qr.InitializationResponse
import org.microg.gms.recaptcha.qr.RecaptchaSignals

internal data class SignalCollectionRequest(
    val action: String,
    val initializationResponse: InitializationResponse,
    val cachedCredential: CachedInitCredential?,
    val timeoutMs: Long,
)

internal interface SignalCollectionManager {
    suspend fun collectSignals(request: SignalCollectionRequest): RecaptchaSignals

    suspend fun refreshSignals(request: SignalCollectionRequest): RecaptchaSignals = collectSignals(request)
}
