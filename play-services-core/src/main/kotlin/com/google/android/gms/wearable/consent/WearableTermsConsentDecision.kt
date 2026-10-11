/* SPDX-License-Identifier: Apache-2.0 */
package com.google.android.gms.wearable.consent

import android.content.Intent

/** Single-use decision: neither a saved value nor a caller flag constitutes an acceptance action. */
class WearableTermsConsentDecision(
    private val enforceCaller: () -> Unit,
    private val persistAcceptance: (beforeCommit: () -> Unit) -> Unit
) {
    private var complete = false

    @Synchronized
    fun accept(checked: Boolean): Boolean {
        if (complete || !checked) return false
        complete = true
        return try {
            enforceCaller()
            // Persistence rechecks authorization inside its transaction, so revocation rolls it back.
            persistAcceptance(enforceCaller)
            true
        } catch (_: Exception) { false }
    }

    @Synchronized
    fun cancel() { complete = true }
}

/** The global, standard terms contract only. Watch-specific and supervised scopes need their own UI. */
object WearableTermsConsentRequest {
    const val ACTION = "com.google.android.gms.wearable.TOS"

    fun validate(intent: Intent?) {
        val request = requireNotNull(intent) { "Missing terms request" }
        require(request.action == ACTION) { "Unsupported terms action" }
        val extras = request.extras
        @Suppress("DEPRECATION")
        val termsContext = extras?.get("terms_context")
        require(termsContext == null || termsContext is Int && termsContext == 0) { "Unsupported terms context" }
        for (name in arrayOf("use_consent_per_watch", "show_backup_consent", "is_watch_supervised")) {
            @Suppress("DEPRECATION")
            val flag = extras?.get(name)
            require(flag == null || flag is Boolean && !flag) { "Unsupported terms scope" }
        }
    }
}
