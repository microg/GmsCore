/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core

import android.content.Context
import android.content.SharedPreferences
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthManagerKeyAcknowledgementTest {
    private lateinit var context: Context
    private lateinit var preferences: SharedPreferences

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        assertTrue(preferences.edit().clear().commit())
    }

    @After
    fun tearDown() {
        preferences.edit().clear().commit()
    }

    @Test
    fun validKeyPairRetainsServerAcknowledgement() {
        context.authManager.getOrCreateKeyPair()
        assertTrue(preferences.edit().putBoolean(KEY_PUBLIC_KEY_ACKED, true).commit())

        assertTrue(ConstellationStateStore.isPublicKeyAcked(context))
    }

    @Test
    fun corruptKeyPairIsNotTreatedAsServerAcknowledged() {
        context.authManager.getOrCreateKeyPair()
        assertTrue(
            preferences.edit()
                .putString(KEY_PRIVATE, "not-a-private-key")
                .putBoolean(KEY_PUBLIC_KEY_ACKED, true)
                .commit()
        )

        assertFalse(ConstellationStateStore.isPublicKeyAcked(context))
    }

    private companion object {
        const val PREFS_NAME = "constellation_prefs"
        const val KEY_PRIVATE = "private_key"
        const val KEY_PUBLIC_KEY_ACKED = "is_public_key_acked"
    }
}
