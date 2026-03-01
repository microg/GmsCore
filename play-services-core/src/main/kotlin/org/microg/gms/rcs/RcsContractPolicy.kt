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
    fun decide(row: ContractRow, config: RcsPolicyConfig): ContractDecision {
        val normalizedCaller = row.callingPackage.lowercase(Locale.US)
        if (!config.messagesClients.contains(normalizedCaller)) {
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
        if (config.enableMinimalCompletion && config.matchesCompletionRow(normalized, row.code)) {
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
