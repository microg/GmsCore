/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.constellation.core

/** Never send an empty Sync, or verify another SIM when the requested SIM is inaccessible. */
internal fun <T> selectVerificationSubscriptions(
    subscriptions: Map<String, T>,
    targetedImsis: List<String>
): Map<String, T> {
    check(subscriptions.isNotEmpty()) {
        "No readable active SIM. Grant Phone access and, where supported, ICC authentication with device identifiers."
    }
    if (targetedImsis.isEmpty()) return subscriptions
    require(targetedImsis.all { it.isNotBlank() && it in subscriptions }) {
        "A requested SIM is unavailable or its identifier cannot be read"
    }
    return subscriptions.filterKeys { it in targetedImsis }
}
