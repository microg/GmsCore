/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core.verification

import android.content.Context
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.Mockito

class MtSmsInboxScopeTest {

    /** Context is only ever handed to the inbox factory, which ignores it, so a bare mock is fine. */
    private val context: Context = Mockito.mock(Context::class.java)

    private class FakeInbox(val subId: Int) : MtSmsInboxHandle {
        var disposed = false
            private set

        override suspend fun awaitMatch(expectedBody: String, requiredSubId: Int): ReceivedSms? = null

        override fun dispose() {
            disposed = true
        }
    }

    private fun recordingScope(): Pair<MtSmsInboxScope, MutableList<FakeInbox>> {
        val created = mutableListOf<FakeInbox>()
        val scope = MtSmsInboxScope { _, subId -> FakeInbox(subId).also { created += it } }
        return scope to created
    }

    @Test
    fun prepare_installsOnePhysicalInboxForAllLogicalSubscriptions() {
        val (scope, created) = recordingScope()
        scope.prepare(context, listOf(1, 2, -1))

        assertEquals(1, created.size)
        assertEquals(-1, created.single().subId)
        assertSame(created.single(), scope.get(1))
        assertSame(created.single(), scope.get(2))
        assertSame(created.single(), scope.get(-1))
    }

    @Test
    fun productionIds_shareOneInboxAcrossActiveAndCatchAllSubscriptions() {
        val (scope, created) = recordingScope()
        scope.prepare(context, mtSmsInboxSubscriptionIds(listOf(1, 2, 1)))

        assertEquals(1, created.size)
        assertSame(scope.get(1), scope.get(2))
        assertSame(scope.get(1), scope.get(-1))
        assertEquals(listOf(-1), mtSmsInboxSubscriptionIds(emptyList()))
    }

    @Test
    fun prepare_withNoSubIds_fallsBackToDefaultSubId() {
        val (scope, _) = recordingScope()
        scope.prepare(context, emptyList())

        // Preserves the original behaviour: an empty sub-id list maps to the "-1" inbox.
        assertEquals(-1, (scope.get(-1) as FakeInbox).subId)
    }

    @Test
    fun prepare_failurePreservesExistingSharedInbox() {
        val failure = IllegalStateException("Receiver registration failed")
        var failCreation = false
        val created = mutableListOf<FakeInbox>()
        val scope = MtSmsInboxScope { _, subId ->
            if (failCreation) throw failure
            FakeInbox(subId).also { created += it }
        }

        scope.prepare(context, listOf(7, -1))
        val existingInbox = created.single()
        failCreation = true

        try {
            scope.prepare(context, listOf(1, 2, -1))
            fail("Expected receiver registration to fail")
        } catch (e: IllegalStateException) {
            assertSame(failure, e)
        }

        assertSame(existingInbox, scope.get(7))
        assertSame(existingInbox, scope.get(-1))
        assertFalse(existingInbox.disposed)

        scope.dispose()
        assertTrue(existingInbox.disposed)
    }

    @Test
    fun get_beforePrepare_throws() {
        val (scope, _) = recordingScope()
        try {
            scope.get(1)
            fail("Expected get() to throw before prepare()")
        } catch (e: IllegalStateException) {
            // expected
        }
    }

    @Test
    fun dispose_disposesOwnInboxes_andIsIdempotent() {
        val (scope, created) = recordingScope()
        scope.prepare(context, listOf(7))
        val inbox = created.single()

        scope.dispose()
        assertTrue(inbox.disposed)

        // Second dispose must not throw and must not touch anything new.
        scope.dispose()
    }

    /**
     * The regression this whole change exists for. A request's `finally { dispose() }` must only
     * clean up its own MT SMS inboxes; it must never dispose the inboxes of a newer, concurrently
     * running request. With the old process-global singleton, `requestB.prepare()` tore down
     * request A, and `requestA.dispose()` then tore down request B. Per-request scopes make both
     * impossible.
     */
    @Test
    fun dispose_cannotEraseANewerRequest() {
        val (requestA, createdA) = recordingScope()
        val (requestB, createdB) = recordingScope()

        // Request A starts and begins waiting on its inbox.
        requestA.prepare(context, listOf(1))
        val inboxA = createdA.single()

        // Request B starts while A is still in flight.
        requestB.prepare(context, listOf(1))
        val inboxB = createdB.single()

        // B starting must not have disposed A's inbox.
        assertFalse("A's inbox was disposed when B started", inboxA.disposed)
        assertSame(inboxA, requestA.get(1))

        // A finishes and runs its cleanup. This must not touch B.
        requestA.dispose()
        assertTrue("A's own inbox should be disposed", inboxA.disposed)
        assertFalse("A's cleanup disposed a newer request's inbox", inboxB.disposed)
        assertSame("B's inbox is still usable after A's cleanup", inboxB, requestB.get(1))
    }

    @Test
    fun registry_resolvesInboxesFromTheCallingCoroutineScope() = runTest {
        val created = mutableListOf<FakeInbox>()
        withContext(MtSmsInboxScope { _, subId -> FakeInbox(subId).also { created += it } }) {
            MtSmsInboxRegistry.prepare(context, listOf(5))
            val handle = MtSmsInboxRegistry.get(5)
            assertSame(created.single(), handle)

            MtSmsInboxRegistry.dispose()
            assertTrue(created.single().disposed)
        }
    }

    @Test
    fun registry_withoutScope_disposeIsNoOp_butGetAndPrepareFail() = runTest {
        // dispose() is called from finally blocks, so it must be safe even with no scope installed.
        MtSmsInboxRegistry.dispose()

        try {
            MtSmsInboxRegistry.prepare(context, listOf(1))
            fail("Expected prepare() to require an MtSmsInboxScope")
        } catch (e: IllegalStateException) {
            // expected
        }
    }
}
