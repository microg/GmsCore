package org.microg.gms.accountaction

sealed class Resolution

/**
 * In this situation, sending a CryptAuth "sync keys" query is sufficient
 * to resolve the problem. This is the case for enterprise accounts that
 * mandate device screenlocks after users enabled checkin, GCM, and
 * configured a lockscreen.
 */
data object CryptAuthSyncKeys : Resolution()

/**
 * Represents a situation in which user actions are required to fix
 * the problem.
 */
data class UserIntervention(val actions: Set<UserAction>) : Resolution()

enum class UserAction {
    ENABLE_CHECKIN,
    ENABLE_GCM,
    ALLOW_MICROG_GCM,
    ENABLE_LOCKSCREEN,
    REAUTHENTICATE
}

/**
 * Represents a situation that is known to be unsupported by microG.
 * Advise the user to remove the account.
 */
data class NoResolution(val reason: NoResolutionReason) : Resolution()

enum class NoResolutionReason {
    ADVANCED_DEVICE_MANAGEMENT_NOT_SUPPORTED
}