/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 *
 * Local cache of account capabilities: decodes the server response into
 * enabled / disabled / pending sets and merges them into AccountManager
 * user-data.
 */
package org.microg.gms.auth.capabilities

import android.accounts.Account
import android.accounts.AccountManager
import org.microg.gms.auth.capabilities.proto.Capabilities
import org.microg.gms.auth.capabilities.proto.CapabilityStatus
import org.microg.gms.auth.capabilities.proto.CapabilityType

data class CapabilityState(
    val enabled: Set<String>,
    val disabled: Set<String>,
    val pending: Set<String>,
    val visibilityByCap: Map<String, List<String>>,
    val syncTimeByCap: Map<String, Long>,
) {
    /** Cache is considered populated once at least one allowed/denied entry exists. */
    val isValidCache: Boolean get() = enabled.isNotEmpty() || disabled.isNotEmpty()
}

object CapabilityStore {

    /** Decode a server response into the enabled/disabled/pending-set form. */
    fun decode(caps: Capabilities, now: Long = System.currentTimeMillis()): CapabilityState {
        val enabled = mutableSetOf<String>()
        val disabled = mutableSetOf<String>()
        val pending = mutableSetOf<String>()
        val vis = mutableMapOf<String, List<String>>()
        val times = mutableMapOf<String, Long>()

        for (c in caps.entries) {
            // Only DEFAULT-typed capabilities are server-managed; skip the rest.
            if (c.type != CapabilityType.TYPE_DEFAULT) continue
            val name = c.name?.takeIf { it.isNotEmpty() } ?: continue

            if (c.visibility.isNotEmpty()) {
                vis[name] = c.visibility.mapNotNull { it.packageName }
            }

            when (c.status) {
                CapabilityStatus.STATUS_DENIED -> {
                    disabled += name; times[name] = now
                }
                CapabilityStatus.STATUS_PENDING -> {
                    pending += name
                }
                // Treat ALLOWED and UNKNOWN the same — default to enabled.
                else -> {
                    enabled += name; times[name] = now
                }
            }
        }
        return CapabilityState(enabled, disabled, pending, vis, times)
    }

    /** Read the current cached state from AccountManager user-data. */
    fun read(am: AccountManager, acc: Account): CapabilityState = CapabilityState(
        enabled = readSet(am, acc, UserDataKeys.ENABLED_CAPS),
        disabled = readSet(am, acc, UserDataKeys.DISABLED_CAPS),
        pending = readSet(am, acc, UserDataKeys.FAILED_CAPS),
        visibilityByCap = decodeVisMap(am.getUserData(acc, UserDataKeys.PACKAGE_VISIBILITY)),
        syncTimeByCap = decodeSyncMap(am.getUserData(acc, UserDataKeys.SYNC_TIME)),
    )

    /**
     * Merge a freshly decoded server state with prior local state and write
     * everything back to AccountManager.UserData.
     *
     * Returns true when an ACCOUNT_CAPABILITIES_CHANGED broadcast should fire.
     */
    fun writeMerged(
        am: AccountManager,
        acc: Account,
        fresh: CapabilityState,
        services: Collection<String>,
    ): Boolean {
        val old = read(am, acc)

        val enabled = fresh.enabled.toMutableSet()
        val disabled = fresh.disabled.toMutableSet()
        val realPending = mutableSetOf<String>()
        for (cap in fresh.pending) when (cap) {
            in old.enabled -> enabled += cap
            in old.disabled -> disabled += cap
            else -> realPending += cap
        }

        am.setUserData(acc, UserDataKeys.ENABLED_CAPS, enabled.joinToString(","))
        am.setUserData(acc, UserDataKeys.DISABLED_CAPS, disabled.joinToString(","))
        am.setUserData(acc, UserDataKeys.FAILED_CAPS, realPending.joinToString(","))
        am.setUserData(acc, UserDataKeys.CAPABILITIES_VERSION, "1")
        am.setUserData(acc, UserDataKeys.PACKAGE_VISIBILITY, encodeVisMap(fresh.visibilityByCap))
        am.setUserData(acc, UserDataKeys.SYNC_TIME, encodeSyncMap(fresh.syncTimeByCap))

        am.setUserData(
            acc, UserDataKeys.HAS_PASSWORD, resolveBoolCap(
                enabled, disabled, UserDataKeys.CAP_HAS_PASSWORD,
                default = am.getUserData(acc, UserDataKeys.HAS_PASSWORD) != "0"
            ).bit()
        )
        am.setUserData(
            acc, UserDataKeys.HAS_USERNAME, resolveBoolCap(
                enabled, disabled, UserDataKeys.CAP_HAS_USERNAME,
                default = am.getUserData(acc, UserDataKeys.HAS_USERNAME) != "0"
            ).bit()
        )

        if (services.isNotEmpty()) {
            am.setUserData(acc, UserDataKeys.SERVICES, services.joinToString(","))
        }

        return old.enabled != enabled ||
                old.disabled != disabled ||
                old.visibilityByCap != fresh.visibilityByCap
    }

    /**
     * Given a local [state] and a set of requested caps, produce a result
     * code matching [HasCapabilitiesResult].
     */
    fun evaluate(state: CapabilityState, request: Collection<String>): Int {
        if (request.isEmpty()) return HasCapabilitiesResult.ALLOWED
        var result = HasCapabilitiesResult.ALLOWED
        for (cap in request) {
            when (cap) {
                in state.enabled -> continue
                in state.disabled -> return HasCapabilitiesResult.DENIED
                in state.pending ->
                    if (result == HasCapabilitiesResult.ALLOWED)
                        result = HasCapabilitiesResult.UNKNOWN
                else -> result = HasCapabilitiesResult.NETWORK_RETRY
            }
        }
        return result
    }

    // ---- Serialization helpers ----

    private fun readSet(am: AccountManager, acc: Account, key: String): Set<String> =
        am.getUserData(acc, key)
            ?.split(',')
            ?.filter { it.isNotEmpty() }
            ?.toHashSet() ?: emptySet()

    private fun encodeVisMap(m: Map<String, List<String>>): String =
        m.toSortedMap().entries.joinToString(";") { (cap, pkgs) ->
            "$cap:${pkgs.toSortedSet().joinToString(",")}"
        }

    private fun decodeVisMap(raw: String?): Map<String, List<String>> {
        if (raw.isNullOrEmpty()) return emptyMap()
        return raw.split(';').mapNotNull {
            val parts = it.split(':', limit = 2)
            if (parts.size != 2) null else parts[0] to parts[1].split(',')
        }.toMap()
    }

    private fun encodeSyncMap(m: Map<String, Long>): String =
        m.flatMap { listOf(it.key, it.value.toString()) }.joinToString(",")

    private fun decodeSyncMap(raw: String?): Map<String, Long> {
        if (raw.isNullOrEmpty()) return emptyMap()
        val parts = raw.split(',')
        if (parts.size % 2 != 0) return emptyMap()
        return (parts.indices step 2).associate { parts[it] to (parts[it + 1].toLongOrNull() ?: 0L) }
    }

    private fun resolveBoolCap(
        enabled: Set<String>, disabled: Set<String>, key: String, default: Boolean
    ): Boolean = when (key) {
        in enabled -> true
        in disabled -> false
        else -> default
    }

    private fun Boolean.bit(): String = if (this) "1" else "0"
}
