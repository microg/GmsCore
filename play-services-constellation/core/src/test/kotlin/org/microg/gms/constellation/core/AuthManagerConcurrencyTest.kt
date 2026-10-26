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
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.security.Signature
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class AuthManagerConcurrencyTest {
    private lateinit var preferences: SharedPreferences
    private lateinit var manager: AuthManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        preferences = context.getSharedPreferences("auth_manager_concurrency_test", Context.MODE_PRIVATE)
        assertTrue(preferences.edit().clear().commit())
        val gatedPreferences = InitialReadBarrier(preferences)
        val isolatedContext = object : ContextWrapper(context) {
            override fun getApplicationContext(): Context = this

            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                if (name == "constellation_prefs") gatedPreferences else super.getSharedPreferences(name, mode)
        }
        manager = AuthManager::class.java.getDeclaredConstructor(Context::class.java).run {
            isAccessible = true
            newInstance(isolatedContext)
        }
    }

    @After
    fun tearDown() {
        preferences.edit().clear().commit()
    }

    @Test
    fun concurrentFirstUseSharesOnePersistedSigningKey() {
        val pairs = loadConcurrently()
        assertArrayEquals(pairs[0].public.encoded, pairs[1].public.encoded)
        assertArrayEquals(pairs[0].private.encoded, pairs[1].private.encoded)
        assertArrayEquals(pairs[0].public.encoded, manager.getOrCreateKeyPair().public.encoded)

        val content = "same key for the published public key and IID signature"
        val signature = manager.sign(content)
        for (pair in pairs) {
            val verifier = Signature.getInstance("SHA256withECDSA")
            verifier.initVerify(pair.public)
            verifier.update(content.toByteArray(StandardCharsets.UTF_8))
            assertTrue(verifier.verify(signature))
        }
    }

    @Test
    fun concurrentReadsRetainExistingKeyAndAcknowledgement() {
        val original = manager.getOrCreateKeyPair()
        assertTrue(preferences.edit().putBoolean("is_public_key_acked", true).commit())

        for (pair in loadConcurrently()) {
            assertArrayEquals(original.public.encoded, pair.public.encoded)
            assertArrayEquals(original.private.encoded, pair.private.encoded)
        }
        assertTrue(preferences.getBoolean("is_public_key_acked", false))
    }

    private fun loadConcurrently(): List<KeyPair> {
        val executor = Executors.newFixedThreadPool(2)
        val start = CountDownLatch(1)
        return try {
            val calls = List(2) {
                executor.submit<KeyPair> {
                    check(start.await(5, TimeUnit.SECONDS))
                    manager.getOrCreateKeyPair()
                }
            }
            start.countDown()
            calls.map { it.get(10, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
        }
    }

    private class InitialReadBarrier(
        private val delegate: SharedPreferences
    ) : SharedPreferences by delegate {
        private val readers = CountDownLatch(2)

        override fun getString(key: String?, defValue: String?): String? {
            val value = delegate.getString(key, defValue)
            if (key == "private_key" && value == null) {
                // Both unsynchronized callers retain the absent read before either can save a key.
                // A serialized caller waits only this bounded interval before generating its key.
                readers.countDown()
                readers.await(250, TimeUnit.MILLISECONDS)
            }
            return value
        }
    }
}
