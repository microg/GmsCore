/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.recaptcha.modac.signals

import okio.ByteString.Companion.toByteString
import org.microg.gms.recaptcha.qr.DroidGuardSignals
import org.microg.gms.recaptcha.qr.EncodedSignal
import org.microg.gms.recaptcha.qr.EncodedSignalBundle
import org.microg.gms.recaptcha.qr.RecaptchaSignals
import org.microg.gms.recaptcha.qr.SignalEncryption
import org.microg.gms.recaptcha.qr.SignalValue
import org.microg.gms.recaptcha.qr.SignalValueList

private const val DROID_GUARD_FORMAT_CODE = 33

internal object SignalEnvelopeBuilder {

    fun build(action: String, droidGuardResult: String? = null): RecaptchaSignals {
        val droidGuardSignals = droidGuardResult
            ?.takeIf { it.isNotEmpty() }
            ?.let { buildDroidGuardSignals(action, it) }
        return RecaptchaSignals(action = action, droidGuardSignals = droidGuardSignals)
    }

    private fun buildDroidGuardSignals(action: String, result: String): DroidGuardSignals {
        val values = SignalValueList(
            values = listOf(
                SignalValue(textValue = result),
                SignalValue(booleanValue = true),
            ),
            formatCode = DROID_GUARD_FORMAT_CODE,
        )
        val signal = EncodedSignal(
            encryption = SignalEncryption.NONE,
            payload = values.encode().toByteString(),
        )
        val bundle = EncodedSignalBundle(signals = listOf(signal), action = action)
        return DroidGuardSignals(signals = bundle)
    }
}
