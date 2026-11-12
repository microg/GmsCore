/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cryptauth

import android.accounts.Account
import android.content.Context
import cryptauthv2.ClientMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

private val IMMEDIATE_CLIENT_NAMES = setOf("ForceRegistration", "ScreenlockSync")
private const val REGISTRATION_TIMEOUT_MILLIS = 60_000L

internal class RegisterForKeyPairOperation(
    private val context: Context,
    private val account: Account,
    private val invocationReason: ClientMetadata.InvocationReason,
    private val keyNames: List<String>,
    private val clientName: String?,
) {
    companion object {
        private val executionMutex = Mutex()
    }

    suspend fun execute(): Boolean {
        if (clientName !in IMMEDIATE_CLIENT_NAMES) return false
        return withTimeoutOrNull(REGISTRATION_TIMEOUT_MILLIS) {
            executionMutex.withLock {
                try {
                    enrollKeyNames(
                        context = context,
                        accountName = account.name,
                        invocationReason = invocationReason,
                        keyNames = keyNames,
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    false
                }
            }
        } ?: false
    }
}
