/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.charset.StandardCharsets
import java.security.Signature

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class AuthManagerSigningFailureTest {
    private lateinit var context: Context
    private lateinit var preferences: SharedPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        preferences = context.getSharedPreferences("auth_manager_signing_test", Context.MODE_PRIVATE)
        assertTrue(preferences.edit().clear().commit())
    }

    @After
    fun tearDown() {
        preferences.edit().clear().commit()
    }

    @Test
    fun iidSigningPropagatesCredentialStorageFailure() {
        val failure = SecurityException("Signing credentials are unavailable")
        val unreadablePreferences = object : SharedPreferences by preferences {
            override fun getString(key: String?, defValue: String?): String? {
                if (key == "private_key") throw failure
                return preferences.getString(key, defValue)
            }
        }
        val manager = isolatedManager(unreadablePreferences)

        assertSame(failure, assertThrows(SecurityException::class.java) {
            manager.signIidTokenCompat("synthetic-iid-token")
        })
    }

    @Test
    fun successfulIidSigningPreservesVerifiablePayloadAndTimestamp() {
        val manager = isolatedManager(preferences)
        val (signature, timestamp) = manager.signIidTokenCompat("synthetic-iid-token")
        val payload = "synthetic-iid-token:${timestamp / 1000}:${(timestamp % 1000) * 1_000_000}"
        val verifier = Signature.getInstance("SHA256withECDSA")
        verifier.initVerify(manager.getOrCreateKeyPair().public)
        verifier.update(payload.toByteArray(StandardCharsets.UTF_8))

        assertTrue(signature.isNotEmpty())
        assertTrue(verifier.verify(signature))
    }

    private fun isolatedManager(signingPreferences: SharedPreferences): AuthManager {
        val isolatedContext = object : ContextWrapper(context) {
            override fun getApplicationContext(): Context = this

            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                if (name == "constellation_prefs") signingPreferences else super.getSharedPreferences(name, mode)
        }
        return AuthManager::class.java.getDeclaredConstructor(Context::class.java).run {
            isAccessible = true
            newInstance(isolatedContext)
        }
    }
}
