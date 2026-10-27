/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.cryptauth

import android.system.Os
import android.accounts.Account
import android.test.InstrumentationTestCase
import cryptauthv2.KeyType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString
import java.io.File
import java.io.IOException
import java.util.UUID
import java.security.interfaces.ECPublicKey
import org.microg.gms.cryptauth.proto.BootstrapScreenLockSignals
import org.microg.gms.cryptauth.proto.BootstrapSourceSignals
import org.microg.gms.cryptauth.proto.Header
import org.microg.gms.cryptauth.proto.HeaderAndBodyInternal
import org.microg.gms.cryptauth.proto.SecureMessage

/** Exercises the actual API21+ filesystem. Only unique synthetic accounts/keys are used. */
@Suppress("DEPRECATION")
class CryptAuthKeyStoreTest : InstrumentationTestCase() {
    fun testPrivateDurableCounterSurvivesReconstructionAndConcurrentStores() = runBlocking {
        withTestAccount { account, base ->
            val store = CryptAuthKeyStore(instrumentation.targetContext, account)
            val key = syntheticKey()
            cryptAuthStateMutex.withLock { store.write(CryptAuthState("filesystem-fixture", keys = listOf(key))) }
            assertEquals(0x180, Os.stat(base.path).st_mode and 0x1ff)
            assertEquals(0x1c0, Os.stat(base.parent).st_mode and 0x1ff)
            val seed = CryptAuthOtpCounter.initialCounter(key.secret)
            val actual = List(16) { async(Dispatchers.IO) {
                CryptAuthOtpCounter(CryptAuthKeyStore(instrumentation.targetContext, account))
                    .useNextCounter(key.handle) { _, count -> count }
            } }.awaitAll()
            assertEquals((seed until seed + 16).toList(), actual.sorted())
            val persisted = cryptAuthStateMutex.withLock { CryptAuthKeyStore(instrumentation.targetContext, account).read() }
            assertEquals(seed + 16, persisted.otpCounters.getValue(key.handle))
        }
    }

    fun testUnwritableTemporaryFileDoesNotReleaseCounterOrDamagePreviousState() = runBlocking {
        withTestAccount { account, base ->
            val store = CryptAuthKeyStore(instrumentation.targetContext, account)
            val key = syntheticKey()
            val previous = CryptAuthState("filesystem-fixture", keys = listOf(key))
            cryptAuthStateMutex.withLock { store.write(previous) }
            // Real open(2) failure, not a fake store: a nonempty directory cannot be a write file.
            val obstruction = File(base.path + ".new")
            assertTrue(obstruction.mkdir())
            val child = File(obstruction, "fixture")
            child.writeText("not-account-data")
            var called = false
            try {
                CryptAuthOtpCounter(store).useNextCounter(key.handle) { _, _ -> called = true }
                fail("Expected filesystem failure")
            } catch (_: IOException) {
                assertFalse(called)
                assertEquals(previous, CryptAuthStateCodec.decode(base.readBytes()))
            } finally {
                assertTrue(child.delete())
                assertTrue(obstruction.delete())
            }
            assertEquals(CryptAuthOtpCounter.initialCounter(key.secret),
                CryptAuthOtpCounter(store).useNextCounter(key.handle) { _, count -> count })
        }
    }

    fun testLostResponseDoesNotRollbackOnRealFilesystem() = runBlocking {
        withTestAccount { account, _ ->
            val store = CryptAuthKeyStore(instrumentation.targetContext, account)
            val key = syntheticKey()
            cryptAuthStateMutex.withLock { store.write(CryptAuthState("filesystem-fixture", keys = listOf(key))) }
            try {
                CryptAuthOtpCounter(store).useNextCounter(key.handle) { _, _ -> throw IOException("lost response") }
                fail("Expected lost response")
            } catch (_: IOException) { }
            assertEquals(CryptAuthOtpCounter.initialCounter(key.secret) + 1,
                CryptAuthOtpCounter(CryptAuthKeyStore(instrumentation.targetContext, account))
                    .useNextCounter(key.handle) { _, count -> count })
        }
    }

    fun testSyntheticAssertionRequiresCurrentAuthorizationAndUsesSecureMessage() = runBlocking {
        withTestAccount { account, _ ->
            val store = CryptAuthKeyStore(instrumentation.targetContext, account)
            val key = syntheticKey()
            val pair = CryptAuthCrypto.generateKeyPair()
            val identity = CryptAuthKey("PublicKey", CryptAuthEnrollment.DEVICE_KEY_HANDLE, KeyType.P256,
                CryptAuthCrypto.publicKey(pair.public as ECPublicKey), pair.private.encoded.toByteString(), true)
            cryptAuthStateMutex.withLock { store.write(CryptAuthState("filesystem-fixture", keys = listOf(identity, key))) }
            var authorizationChecks = 0
            val selectedAccount = Account(account, "com.google")
            val authorization = object : CryptAuthBootstrapAuthorization {
                override val account = selectedAccount
                override fun enforceCurrent() { authorizationChecks++ }
            }
            val confirmedHandle = CryptAuthBootstrapAssertions.readConfirmedAuthzenHandle(store, identity.publicKey)
            assertEquals(key.handle, confirmedHandle)
            assertTrue(cryptAuthStateMutex.withLock { store.read().otpCounters.isEmpty() })
            val result = CryptAuthBootstrapAssertions.create(store, confirmedHandle, identity.publicKey, authorization,
                "synthetic-challenge".encodeUtf8(), syntheticSignals())
            assertEquals(3, authorizationChecks)
            val message = SecureMessage.ADAPTER.decode(result.encryptedAssertion)
            val header = Header.ADAPTER.decode(HeaderAndBodyInternal.ADAPTER.decode(message.header_and_body).header_)
            assertEquals("080b1001", header.public_metadata!!.hex())
            assertEquals(identity.publicKey, header.verification_key_id)
            assertTrue(result.clientData.utf8().contains(account))
            assertEquals(CryptAuthOtpCounter.initialCounter(key.secret) + 1,
                cryptAuthStateMutex.withLock { store.read().otpCounters.getValue(key.handle) })
        }
    }

    fun testMissingSignalsOrExpiredConsentCannotReleaseAnAssertion() = runBlocking {
        withTestAccount { account, _ ->
            val store = CryptAuthKeyStore(instrumentation.targetContext, account)
            val key = syntheticKey()
            val pair = CryptAuthCrypto.generateKeyPair()
            val identity = CryptAuthKey("PublicKey", CryptAuthEnrollment.DEVICE_KEY_HANDLE, KeyType.P256,
                CryptAuthCrypto.publicKey(pair.public as ECPublicKey), pair.private.encoded.toByteString(), true)
            cryptAuthStateMutex.withLock { store.write(CryptAuthState("filesystem-fixture", keys = listOf(identity, key))) }
            var checks = 0
            var failAt = Int.MAX_VALUE
            val selectedAccount = Account(account, "com.google")
            val authorization = object : CryptAuthBootstrapAuthorization {
                override val account = selectedAccount
                override fun enforceCurrent() {
                    checks++
                    if (checks >= failAt) throw SecurityException("Synthetic expired consent")
                }
            }
            for (invalid in listOf(syntheticSignals().copy(droidguard_result = ""),
                syntheticSignals().copy(screen_lock = syntheticSignals().screen_lock!!.copy(screen_lock_confirmed = false)))) {
                try {
                    CryptAuthBootstrapAssertions.create(store, key.handle, identity.publicKey, authorization, "challenge".encodeUtf8(), invalid)
                    fail("Expected rejected signals")
                } catch (_: IllegalArgumentException) { }
            }
            assertTrue(cryptAuthStateMutex.withLock { store.read().otpCounters.isEmpty() })
            checks = 0; failAt = 1
            try {
                CryptAuthBootstrapAssertions.create(store, key.handle, identity.publicKey, authorization, "challenge".encodeUtf8(), syntheticSignals())
                fail("Expected expired consent")
            } catch (_: SecurityException) { }
            assertTrue(cryptAuthStateMutex.withLock { store.read().otpCounters.isEmpty() })
            checks = 0; failAt = 3 // Consent expires after signing; no assertion may escape.
            try {
                CryptAuthBootstrapAssertions.create(store, key.handle, identity.publicKey, authorization, "challenge".encodeUtf8(), syntheticSignals())
                fail("Expected expired consent")
            } catch (_: SecurityException) { }
            assertEquals(CryptAuthOtpCounter.initialCounter(key.secret) + 1,
                cryptAuthStateMutex.withLock { store.read().otpCounters.getValue(key.handle) })
        }
    }


    fun testChangedAdvertisedIdentityCannotReleaseAnAssertion() = runBlocking {
        withTestAccount { account, _ ->
            val store = CryptAuthKeyStore(instrumentation.targetContext, account)
            val key = syntheticKey()
            val pair = CryptAuthCrypto.generateKeyPair()
            val identity = CryptAuthKey("PublicKey", CryptAuthEnrollment.DEVICE_KEY_HANDLE, KeyType.P256,
                CryptAuthCrypto.publicKey(pair.public as ECPublicKey), pair.private.encoded.toByteString(), true)
            cryptAuthStateMutex.withLock { store.write(CryptAuthState("filesystem-fixture", keys = listOf(identity, key))) }
            val selectedAccount = Account(account, "com.google")
            val authorization = object : CryptAuthBootstrapAuthorization {
                override val account = selectedAccount
                override fun enforceCurrent() { }
            }
            val previouslyAdvertised = CryptAuthCrypto.publicKey(CryptAuthCrypto.generateKeyPair().public as ECPublicKey)
            try {
                CryptAuthBootstrapAssertions.create(store, key.handle, previouslyAdvertised, authorization,
                    "challenge".encodeUtf8(), syntheticSignals())
                fail("Expected advertised identity mismatch rejection")
            } catch (_: IllegalArgumentException) { }
            // A reserved counter stays consumed even when session checks reject the assertion.
            assertEquals(CryptAuthOtpCounter.initialCounter(key.secret) + 1,
                cryptAuthStateMutex.withLock { store.read().otpCounters.getValue(key.handle) })
        }
    }

    private fun syntheticSignals() = BootstrapSourceSignals(android_id = "1234", device = "test", model = "test",
        sdk_version = "34", gms_version = "123", droidguard_result = "synthetic-only-never-sent",
        screen_lock = BootstrapScreenLockSignals(secure_screen_lock = true, screen_lock_confirmed = true,
            screen_lock_state = 2, seconds_since_unlock = -1, seconds_since_screen_lock_setup = -1))

    private fun syntheticKey() = ByteArray(32) { it.toByte() }.toByteString().let { secret ->
        CryptAuthKey("authzen", secret.sha256().base64Url().encodeUtf8(), KeyType.RAW256, secret = secret, active = true)
    }

    private suspend fun withTestAccount(block: suspend (String, File) -> Unit) {
        val account = "instrumentation-cryptauth-${UUID.randomUUID()}"
        val base = File(File(instrumentation.targetContext.noBackupFilesDir, "cryptauth"), account.encodeUtf8().sha256().hex())
        try { block(account, base) } finally {
            // Only this test's exact files, never the parent or another account's state.
            for (suffix in listOf("", ".new", ".bak")) {
                val owned = File(base.path + suffix)
                if (owned.exists()) assertTrue(owned.delete())
            }
        }
    }
}
