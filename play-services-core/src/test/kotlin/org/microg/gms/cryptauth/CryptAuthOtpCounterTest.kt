/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.cryptauth

import cryptauthv2.KeyType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.nio.ByteBuffer

class CryptAuthOtpCounterTest {
    private val first = key(ByteArray(32) { it.toByte() }.toByteString())
    private val seed = 318368841243183923L

    @Test fun derivationMatchesIndependentHmacFixtures() {
        // Independent Python stdlib oracle: HMAC-SHA256 extract+one-block expand, twice.
        // salt=SHA256(b'GoogleOfflineOTP'); first info=b'THOTP'; second=b'THOTP-Counter'.
        // unpack('<Q', output[:8]) & ((1 << 63)-1). No Google account/material is involved.
        assertEquals(seed, CryptAuthOtpCounter.initialCounter(first.secret))
        assertEquals(2829881200097377191L, CryptAuthOtpCounter.initialCounter(ByteArray(32).toByteString()))
        assertEquals(895063351524979067L, CryptAuthOtpCounter.initialCounter(ByteArray(32) { -1 }.toByteString()))
        rejected { CryptAuthOtpCounter.initialCounter(ByteArray(16).toByteString()) }
    }

    @Test fun advancesDurablyBeforeUseAndSurvivesRestart() = runBlocking {
        val store = MemoryStore(state())
        CryptAuthOtpCounter(store).useNextCounter(first.handle) { _, counter ->
            assertEquals(seed, counter)
            assertEquals(seed + 1, store.read().otpCounters.getValue(first.handle))
        }
        val restarted = MemoryStore(CryptAuthStateCodec.decode(store.bytes.copyOf()))
        assertEquals(seed + 1, CryptAuthOtpCounter(restarted).useNextCounter(first.handle) { _, count -> count })
    }

    @Test fun independentReservationsAreUniqueAcrossConcurrentInstances() = runBlocking {
        val store = MemoryStore(state())
        val counters = List(64) { async(Dispatchers.Default) {
            CryptAuthOtpCounter(store).useNextCounter(first.handle) { _, count -> count }
        } }.awaitAll()
        assertEquals((seed until seed + 64).toList(), counters.sorted())
        assertEquals(seed + 64, store.read().otpCounters.getValue(first.handle))
    }

    @Test fun lostResponseAndCancelledUseNeverReuseCounter() = runBlocking {
        val store = MemoryStore(state())
        val counter = CryptAuthOtpCounter(store)
        try { counter.useNextCounter(first.handle) { _, _ -> throw IOException("response lost") }; fail() }
        catch (_: IOException) { }
        try { counter.useNextCounter(first.handle) { _, _ -> throw CancellationException("cancelled") }; fail() }
        catch (_: CancellationException) { }
        assertEquals(seed + 2, counter.useNextCounter(first.handle) { _, count -> count })
    }

    @Test fun failedPersistenceDoesNotReleaseAnAssertion() = runBlocking {
        val store = MemoryStore(state()).apply { failWrite = true }
        var called = false
        try { CryptAuthOtpCounter(store).useNextCounter(first.handle) { _, _ -> called = true }; fail() }
        catch (_: IOException) { }
        assertFalse(called)
        assertTrue(store.read().otpCounters.isEmpty())
        assertEquals(seed, CryptAuthOtpCounter(store).useNextCounter(first.handle) { _, count -> count })
    }

    @Test fun rotationStartsNewKeySequenceAndReactivationContinuesOldSequence() = runBlocking {
        val second = key(ByteArray(32) { -1 }.toByteString())
        val store = MemoryStore(state())
        val counter = CryptAuthOtpCounter(store)
        counter.useNextCounter(first.handle) { _, _ -> }
        store.write(store.read().copy(keys = listOf(first.copy(active = false), second)))
        rejectedSuspend { counter.useNextCounter(first.handle) { _, _ -> fail() } }
        assertEquals(895063351524979067L, counter.useNextCounter(second.handle) { _, count -> count })
        store.write(store.read().copy(keys = listOf(first, second.copy(active = false))))
        assertEquals(seed + 1, counter.useNextCounter(first.handle) { _, count -> count })
    }

    @Test fun unconfirmedRevokedMalformedAndAmbiguousKeysAreRejected() = runBlocking {
        for (invalid in listOf(
            state().copy(pendingKeys = listOf(first)),
            state().copy(keys = emptyList()),
            state().copy(keys = listOf(first.copy(active = false))),
            state().copy(keys = listOf(first.copy(type = KeyType.RAW128))),
            state().copy(keys = listOf(first.copy(handle = "wrong".encodeUtf8()))),
            state().copy(keys = listOf(first, first)),
            state().copy(otpCounters = mapOf(first.handle to -1L))
        )) {
            // Deliberately bypass the codec so the operation must validate even a broken store.
            val store = object : CryptAuthStateStore {
                override fun read() = invalid
                override fun write(state: CryptAuthState) { fail("Invalid state must not be persisted") }
            }
            rejectedSuspend { CryptAuthOtpCounter(store).useNextCounter(first.handle) { _, _ -> fail() } }
        }
    }

    @Test fun maximumCounterWrapsToZeroWithoutSignedOverflow() = runBlocking {
        val store = MemoryStore(state().copy(otpCounters = mapOf(first.handle to Long.MAX_VALUE)))
        assertEquals(Long.MAX_VALUE, CryptAuthOtpCounter(store).useNextCounter(first.handle) { _, count -> count })
        assertEquals(0L, CryptAuthOtpCounter(store).useNextCounter(first.handle) { _, count -> count })
    }

    @Test fun accountsWithSameTestKeyHaveIndependentStores() = runBlocking {
        val accountA = MemoryStore(state())
        val accountB = MemoryStore(state().copy(instanceId = "another-account"))
        CryptAuthOtpCounter(accountA).useNextCounter(first.handle) { _, _ -> }
        assertTrue(accountB.read().otpCounters.isEmpty())
        assertEquals(seed, CryptAuthOtpCounter(accountB).useNextCounter(first.handle) { _, count -> count })
    }

    @Test fun versionOneStateMigratesAndMalformedCounterStateFailsClosed() {
        val current = CryptAuthStateCodec.encode(state())
        // Version one has the same body, without the final counter count.
        val legacy = current.copyOf(current.size - 4)
        ByteBuffer.wrap(legacy).putInt(1)
        assertEquals(state(), CryptAuthStateCodec.decode(legacy))
        val withCounter = state().copy(otpCounters = mapOf(first.handle to seed))
        assertEquals(withCounter, CryptAuthStateCodec.decode(CryptAuthStateCodec.encode(withCounter)))
        rejected { CryptAuthStateCodec.encode(state().copy(otpCounters = mapOf(first.handle to -1))) }
        rejected { CryptAuthStateCodec.encode(state().copy(otpCounters = mapOf("absent".encodeUtf8() to 0))) }
        rejected { CryptAuthStateCodec.decode(current + "0001".decodeHex().toByteArray()) }
    }

    @Test fun stateStringDoesNotExposeKeyOrCounter() {
        val string = state().copy(otpCounters = mapOf(first.handle to seed)).toString()
        assertFalse(string.contains(first.secret.hex()))
        assertFalse(string.contains(first.handle.utf8()))
        assertFalse(string.contains(seed.toString()))
    }

    private fun state() = CryptAuthState("counter-fixture", keys = listOf(first))
    private fun key(secret: ByteString) = CryptAuthKey("authzen", secret.sha256().base64Url().encodeUtf8(),
        KeyType.RAW256, secret = secret, active = true)

    private class MemoryStore(state: CryptAuthState) : CryptAuthStateStore {
        var bytes = CryptAuthStateCodec.encode(state)
        var failWrite = false
        override fun read() = CryptAuthStateCodec.decode(bytes.copyOf())
        override fun write(state: CryptAuthState) {
            if (failWrite) { failWrite = false; throw IOException("storage unavailable") }
            bytes = CryptAuthStateCodec.encode(state)
        }
    }

    private fun rejected(block: () -> Unit) {
        try { block() } catch (_: IllegalArgumentException) { return }
        fail("Expected rejected state")
    }
    private suspend fun rejectedSuspend(block: suspend () -> Unit) {
        try { block() } catch (_: IllegalArgumentException) { return }
        fail("Expected rejected state")
    }
}
