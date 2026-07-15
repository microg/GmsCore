/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core.proto.builder

/**
 * Resolves the SIM-readable phone number sent in Constellation sync requests.
 *
 * Google Messages may supply a phoneNumberHint when E.164 formatting from the
 * subscription record is unavailable (common on dual-SIM or incomplete SIM profiles).
 */
internal fun resolveSimReadableNumber(
    formattedE164: String?,
    phoneNumberHint: String?,
    rawSubscriptionNumber: String?
): String {
    if (!formattedE164.isNullOrBlank()) {
        return formattedE164
    }
    if (!phoneNumberHint.isNullOrBlank()) {
        return phoneNumberHint
    }
    return rawSubscriptionNumber.orEmpty()
}
