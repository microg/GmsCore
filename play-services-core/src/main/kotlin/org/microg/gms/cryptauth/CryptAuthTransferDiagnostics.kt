/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.cryptauth

import android.util.Log
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/** Fixed local checkpoints; never derive diagnostic codes from account or server data. */
internal enum class CryptAuthTransferDiagnosticReason {
    AUTHORIZATION_INITIAL, ACCOUNT_TYPE, STORAGE_API, CHECKIN_READ, CHECKIN_REQUIRED,
    STORAGE_OPEN, STATE_READ, GCM_REGISTRATION, GCM_TOKEN, OAUTH_AUTHENTICATION, OAUTH_TOKEN,
    CLIENT_METADATA, SYNC_KEYS, KEY_ENROLLMENT, ENROLL_KEYS, STATE_RELOAD, TRANSFER_KEYS,
    AUTHORIZATION_FINAL, ACCOUNT_UNCHANGED, IDENTITY_PUBLIC_KEY,
    ASSERTION_CHALLENGE, ASSERTION_IDENTITY, ASSERTION_KEYS, ASSERTION_SCREEN_LOCK,
    ASSERTION_CHECKIN, DROIDGUARD_REQUEST, DROIDGUARD_RESULT, ASSERTION_SIGNALS, ASSERTION_COUNTER_AND_SIGN,
    DROIDGUARD_EMPTY, DROIDGUARD_TOO_LARGE, DROIDGUARD_LOCAL_ERROR
}

/** No messages, causes, custom class names, payloads or identifiers enter this diagnostic. */
internal fun cryptAuthTransferFailureSummary(reason: CryptAuthTransferDiagnosticReason, error: Exception): String {
    val kind = when (error) {
        is CancellationException -> "CancellationException"
        is SecurityException -> "SecurityException"
        is IllegalArgumentException -> "IllegalArgumentException"
        is IllegalStateException -> "IllegalStateException"
        is IOException -> "IOException"
        else -> "OtherException"
    }
    return "${reason.name} failed ($kind)"
}

/** One diagnostic per invocation, including nested checkpoints; failure semantics stay unchanged. */
internal class CryptAuthTransferDiagnostics(
    private val report: (String) -> Unit = { Log.w("CryptAuthTransfer", it) }
) {
    private val recorded = AtomicBoolean()

    suspend fun <T> at(reason: CryptAuthTransferDiagnosticReason, block: suspend () -> T): T = try {
        block()
    } catch (error: Exception) {
        if (recorded.compareAndSet(false, true)) {
            // A logging failure must not replace the original operation or cancellation exception.
            runCatching { report(cryptAuthTransferFailureSummary(reason, error)) }
        }
        throw error
    }
}
