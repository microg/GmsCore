package org.microg.gms.accountaction

import java.io.Serializable

sealed class Resolution

/**
 * In this situation, sending a CryptAuth "sync keys" query is sufficient
 * to resolve the problem. This is the case for enterprise accounts that
 * mandate device screenlocks after users enabled checkin, GCM, and
 * configured a lockscreen.
 */
data object CryptAuthSyncKeys : Resolution()

/**
 * Represents a situation in which user actions are required to satisfy
 * the requirements that need to be fulfilled before the problem can be
 * fixed.
 */
data class UserSatisfyRequirements(val actions: Set<Requirement>) : Resolution(), Serializable

enum class Requirement {
    ENABLE_CHECKIN,
    ENABLE_GCM,
    ALLOW_MICROG_GCM,
    ENABLE_LOCKSCREEN
}

/**
 * Represents a situation in which the user's authentication has become
 * invalid, and they need to enter their credentials again.
 */
data object Reauthenticate : Resolution()

/**
 * Represents a situation that is known to be unsupported by microG.
 * Advise the user to remove the account.
 */
data class NoResolution(val reason: NoResolutionReason) : Resolution()

enum class NoResolutionReason {
    ADVANCED_DEVICE_MANAGEMENT_NOT_SUPPORTED
}