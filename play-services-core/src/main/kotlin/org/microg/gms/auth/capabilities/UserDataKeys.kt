/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 *
 * AccountManager.UserData keys used by the GMS-compatible account
 * capability storage. String values match the on-disk keys the GMS client
 * reads so existing caches remain interoperable.
 */
package org.microg.gms.auth.capabilities

object UserDataKeys {
    const val GOOGLE_USER_ID       = "GoogleUserId"
    const val CAPABILITIES_VERSION = "capabilities_version"
    const val DISABLED_CAPS        = "disabled_capabilities"
    const val ENABLED_CAPS         = "enabled_capabilities"
    const val FAILED_CAPS          = "failed_capabilities"
    const val HAS_PASSWORD         = "hasPassword"
    const val HAS_USERNAME         = "hasUsername"
    const val FIRST_NAME           = "firstName"
    const val LAST_NAME            = "lastName"
    const val SERVICES             = "services"
    const val PACKAGE_VISIBILITY   = "package_visibilities_for_capabilities"
    const val SYNC_TIME            = "capability_sync_time"

    // Pseudo-capabilities that encode boolean account flags.
    const val CAP_HAS_PASSWORD = "geytglldmfya"
    const val CAP_HAS_USERNAME = "geydolldmfya"
}

object CapabilityBroadcasts {
    const val ACTION_CHANGED = "com.google.android.gms.auth.ACCOUNT_CAPABILITIES_CHANGED"
    const val EXTRA_ACCOUNT  = "account"
}

/** Result codes returned by IAuthManagerService.hasCapabilities. */
object HasCapabilitiesResult {
    const val ALLOWED        = 1
    const val DENIED         = 2
    const val UNKNOWN        = 3
    const val VIS_DENIED     = 4
    const val NETWORK_RETRY  = 5
    const val NOT_IN_CACHE   = 6
    const val IO_ERROR       = 8
}
