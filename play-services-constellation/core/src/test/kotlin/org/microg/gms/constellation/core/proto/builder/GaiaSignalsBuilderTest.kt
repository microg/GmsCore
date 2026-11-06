/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core.proto.builder

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.microg.gms.constellation.core.proto.GaiaSignals
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GaiaSignalsBuilderTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val accountManager = AccountManager.get(context)

    private fun addAccount(name: String, googleUserId: String? = null): Account {
        val account = Account(name, "com.google")
        shadowOf(accountManager).addAccount(account)
        if (googleUserId != null) accountManager.setUserData(account, "GoogleUserId", googleUserId)
        return account
    }

    @Test
    fun accountWithoutGoogleUserIdDoesNotDropLaterAccounts() {
        addAccount("first@example.com")
        addAccount("second@example.com", googleUserId = "111")

        val signals = runBlocking { GaiaSignals(context) }

        assertEquals(listOf("111"), signals?.gaia_signals?.map { it.gaia_id })
    }

    @Test
    fun missingGoogleUserIdIsResolvedThroughAccountIdToken() {
        val account = addAccount("user@example.com")
        accountManager.setAuthToken(account, "^^_account_id_^^", "222")

        val signals = runBlocking { GaiaSignals(context) }

        assertEquals(listOf("222"), signals?.gaia_signals?.map { it.gaia_id })
        assertEquals("222", accountManager.getUserData(account, "GoogleUserId"))
    }

    @Test
    fun noResolvableGaiaIdYieldsNoSignals() {
        addAccount("user@example.com")

        assertNull(runBlocking { GaiaSignals(context) })
    }
}
