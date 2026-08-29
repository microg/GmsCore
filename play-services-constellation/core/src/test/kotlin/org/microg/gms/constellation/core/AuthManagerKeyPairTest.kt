/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core

import android.content.Context
import android.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AuthManagerKeyPairTest {

    private val prefs by lazy {
        RuntimeEnvironment.getApplication()
            .getSharedPreferences(CONSTELLATION_EC_PREFS, Context.MODE_PRIVATE)
    }

    @Before
    fun clearPrefs() {
        prefs.edit().clear().commit()
    }

    @Test
    fun regeneratingCorruptKeysClearsPublicKeyAck() {
        prefs.edit()
            .putString(KEY_EC_PRIVATE, "not-a-key")
            .putString(KEY_EC_PUBLIC, "also-not-a-key")
            .putBoolean(KEY_PUBLIC_KEY_ACKED, true)
            .commit()

        val kp = loadOrCreateEcKeyPair(prefs)
        val stored = decodeEcKeyPair(
            prefs.getString(KEY_EC_PRIVATE, null)!!,
            prefs.getString(KEY_EC_PUBLIC, null)!!
        )

        assertFalse(prefs.getBoolean(KEY_PUBLIC_KEY_ACKED, true))
        assertEquals(
            Base64.encodeToString(kp.public.encoded, Base64.NO_WRAP),
            Base64.encodeToString(stored.public.encoded, Base64.NO_WRAP)
        )
    }

    @Test
    fun regeneratingMissingKeysClearsPublicKeyAck() {
        prefs.edit()
            .putBoolean(KEY_PUBLIC_KEY_ACKED, true)
            .commit()

        loadOrCreateEcKeyPair(prefs)

        assertFalse(prefs.getBoolean(KEY_PUBLIC_KEY_ACKED, true))
        assertTrue(!prefs.getString(KEY_EC_PRIVATE, null).isNullOrEmpty())
        assertTrue(!prefs.getString(KEY_EC_PUBLIC, null).isNullOrEmpty())
    }

    @Test
    fun validKeysKeepPublicKeyAck() {
        val original = loadOrCreateEcKeyPair(prefs)
        prefs.edit().putBoolean(KEY_PUBLIC_KEY_ACKED, true).commit()

        val reloaded = loadOrCreateEcKeyPair(prefs)

        assertTrue(prefs.getBoolean(KEY_PUBLIC_KEY_ACKED, false))
        assertEquals(
            Base64.encodeToString(original.public.encoded, Base64.NO_WRAP),
            Base64.encodeToString(reloaded.public.encoded, Base64.NO_WRAP)
        )
        assertNotEquals("not-a-key", prefs.getString(KEY_EC_PRIVATE, null))
    }
}
