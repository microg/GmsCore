/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs

import java.util.Locale

internal data class ContractRow(
    val token: String?,
    val code: Int,
    val callingPackage: String
)

internal enum class ContractDecisionMode {
    UNHANDLED,
    OBSERVE_CONFIG,
    OBSERVE_GENERIC,
    COMPLETE_CONFIG_UNAVAILABLE,
    COMPLETE_GENERIC_UNAVAILABLE,
    REJECT_NON_MESSAGES_CLIENT
}

internal data class ContractDecision(
    val mode: ContractDecisionMode,
    val detail: String,
    val handled: Boolean
)

internal object RcsContractPolicy {
    // Keep enabled for research iterations; this does not claim end-to-end success,
    // it only returns deterministic unavailable semantics for a narrow row set.
    private const val ENABLE_MINIMAL_COMPLETION = true

    private val messagesClients = setOf(
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging"
    )
    private val completionRows = setOf(
        Pair("com.google.android.gms.rcs.iprovisioning", 1),
        Pair("com.google.android.gms.rcs.iprovisioning", 2),
        Pair("com.google.android.gms.rcs.iprovisioning", 1001)
    )

    fun decide(row: ContractRow): ContractDecision {
        if (!messagesClients.contains(row.callingPackage)) {
            return ContractDecision(
                mode = ContractDecisionMode.REJECT_NON_MESSAGES_CLIENT,
                detail = "reject_non_messages_client",
                handled = false
            )
        }
        val token = row.token ?: return ContractDecision(
            mode = ContractDecisionMode.UNHANDLED,
            detail = "passthrough",
            handled = false
        )
        if (!isKnownRcsContract(token)) {
            return ContractDecision(
                mode = ContractDecisionMode.UNHANDLED,
                detail = "passthrough",
                handled = false
            )
        }
        val mode = if (row.code == 1 || row.code == 2 || row.code == 1001) {
            ContractDecisionMode.OBSERVE_CONFIG
        } else {
            ContractDecisionMode.OBSERVE_GENERIC
        }
        val normalized = token.lowercase(Locale.US)
        if (ENABLE_MINIMAL_COMPLETION && completionRows.contains(Pair(normalized, row.code))) {
            val completionMode = if (mode == ContractDecisionMode.OBSERVE_CONFIG) {
                ContractDecisionMode.COMPLETE_CONFIG_UNAVAILABLE
            } else {
                ContractDecisionMode.COMPLETE_GENERIC_UNAVAILABLE
            }
            return ContractDecision(
                mode = completionMode,
                detail = if (completionMode == ContractDecisionMode.COMPLETE_CONFIG_UNAVAILABLE) {
                    "complete_config_unavailable"
                } else {
                    "complete_generic_unavailable"
                },
                handled = true
            )
        }
        return ContractDecision(
            mode = mode,
            detail = if (mode == ContractDecisionMode.OBSERVE_CONFIG) "observe_config_request" else "observe_generic_request",
            handled = false
        )
    }

    private fun isKnownRcsContract(token: String): Boolean {
        val normalized = token.lowercase(Locale.US)
        if (!normalized.startsWith("com.google.android")) return false
        return normalized.contains(".rcs.") ||
            normalized.contains(".carrierauth.") ||
            normalized.contains("provisioning")
    }
}
