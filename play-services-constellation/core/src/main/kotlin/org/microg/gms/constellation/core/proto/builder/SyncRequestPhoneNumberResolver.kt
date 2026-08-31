/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core.proto.builder

/**
 * Resolves the E.164 number to send in a Constellation SyncRequest.
 *
 * Some devices (and some SIMs) expose a blank [SubscriptionInfo.number]. Messages
 * still passes a phoneNumberHint on the ImsiRequest; using that hint prevents
 * provisioning from aborting on a null E.164.
 */
object SyncRequestPhoneNumberResolver {
    fun resolve(
        simNumber: String?,
        countryIso: String?,
        hint: String?,
        formatE164: (number: String, iso: String) -> String?
    ): String {
        val formattedSim = simNumber
            ?.takeIf { it.isNotBlank() }
            ?.let { formatE164(it, countryIso.orEmpty()) }
        if (!formattedSim.isNullOrEmpty()) return formattedSim
        if (hint.isNullOrBlank()) return ""
        return formatE164(hint, countryIso.orEmpty()) ?: hint
    }
}
