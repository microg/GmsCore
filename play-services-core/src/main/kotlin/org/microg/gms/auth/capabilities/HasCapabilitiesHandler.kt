/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 *
 * Top-level handler for IAuthManagerService.hasCapabilities. Combines a
 * local-cache fast-path with a network sync against the account_state
 * endpoint.
 */
package org.microg.gms.auth.capabilities

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.auth.HasCapabilitiesRequest
import java.util.concurrent.ConcurrentHashMap

class HasCapabilitiesHandler(private val context: Context) {

    private val am = AccountManager.get(context)

    /**
     * Returns a result code:
     *   1 = allowed, 2 = denied, 3 = unknown, 4 = visibility-denied,
     *   5 = network-retry, 6 = not-in-cache, 8 = IO error.
     */
    fun handle(request: HasCapabilitiesRequest): Int {
        val account: Account = request.account ?: return HasCapabilitiesResult.NOT_IN_CACHE
        val caps = request.capabilities?.toSet().orEmpty()
        if (caps.isEmpty()) return HasCapabilitiesResult.ALLOWED

        // 1) services-list short-circuit: caps that correspond to a service
        //    already known on this account are considered granted.
        val services = am.getUserData(account, UserDataKeys.SERVICES)
            ?.split(',')?.filter { it.isNotEmpty() }?.toSet().orEmpty()
        Log.d(TAG, "handle: caps=$caps services=$services")
        val unresolved = caps.filterNot { cap ->
            if (cap.startsWith("service_"))
                services.contains(cap.removePrefix("service_"))
            else
                services.contains(cap)
        }
        if (unresolved.isEmpty()) return HasCapabilitiesResult.ALLOWED

        // 2) Local capability cache
        var state = CapabilityStore.read(am, account)

        // 3) Force a sync if the cache is empty.
        if (!state.isValidCache) {
            if (!syncOnce(account)) return HasCapabilitiesResult.NOT_IN_CACHE
            state = CapabilityStore.read(am, account)
            if (!state.isValidCache) return HasCapabilitiesResult.NOT_IN_CACHE
        }

        // 4) Evaluate, and if the answer is NETWORK_RETRY refresh one more time.
        val first = CapabilityStore.evaluate(state, unresolved)
        if (first != HasCapabilitiesResult.NETWORK_RETRY) return first

        syncOnce(account)
        state = CapabilityStore.read(am, account)
        return CapabilityStore.evaluate(state, unresolved)
    }

    /**
     * Fetch the latest state, merge it into AccountManager, and emit the
     * broadcast. Returns false on any error.
     */
    private fun syncOnce(account: Account): Boolean {
        val lock = lockFor(account)
        return synchronized(lock) {
            // Throttle: if a previous sync for this account succeeded within
            // [SYNC_THROTTLE_MS], reuse that result. Failed syncs do not
            // update the timestamp, so offline/retry paths are unaffected.
            val since = SystemClock.elapsedRealtime() - (lastSyncAt[account] ?: 0L)
            if (since < SYNC_THROTTLE_MS) {
                Log.d(TAG, "syncOnce: throttled (${since}ms since last success)")
                return@synchronized true
            }
            try {
                Log.d(TAG, "syncOnce: ")
                val resp = AccountStateClient(context).sync(account)
                val fresh = resp.capabilities?.let { CapabilityStore.decode(it) }
                    ?: CapabilityState(emptySet(), emptySet(), emptySet(), emptyMap(), emptyMap())

                val changed = CapabilityStore.writeMerged(am, account, fresh, resp.services)

                if (changed) {
                    context.sendBroadcast(
                        Intent(CapabilityBroadcasts.ACTION_CHANGED)
                            .putExtra(CapabilityBroadcasts.EXTRA_ACCOUNT, account)
                    )
                }
                lastSyncAt[account] = SystemClock.elapsedRealtime()
                true
            } catch (e: Exception) {
                Log.w(TAG, "Account state sync failed: ${e.message}")
                false
            }
        }
    }

    /**
     * API-19-safe get-or-put for [accountLocks]. `computeIfAbsent` requires
     * Android API 24+, so we use [ConcurrentHashMap.putIfAbsent] (available
     * since API 1) instead.
     */
    private fun lockFor(account: Account): Any {
        accountLocks[account]?.let { return it }
        val candidate = Any()
        return accountLocks.putIfAbsent(account, candidate) ?: candidate
    }

    companion object {
        private const val TAG = "HasCapabilitiesHandler"

        /**
         * Time window after a successful sync during which duplicate
         * syncOnce calls for the same account are short-circuited. 5 s
         * collapses the burst of concurrent hasCapabilities calls that
         * typically follows first-time account login, while leaving any
         * manual refresh issued more than 5 s later unaffected.
         */
        private const val SYNC_THROTTLE_MS = 5_000L

        /**
         * Per-account mutex. Shared across all [HasCapabilitiesHandler]
         * instances in this process — the handler itself is instantiated
         * per-call in [AuthManagerServiceImpl], so the lock map must be
         * process-scoped. Entries are never removed; the set is bounded by
         * the number of Google accounts on the device (typically 1-3).
         */
        private val accountLocks = ConcurrentHashMap<Account, Any>()

        /** Timestamp (elapsedRealtime) of the most recent successful sync. */
        private val lastSyncAt = ConcurrentHashMap<Account, Long>()
    }
}
