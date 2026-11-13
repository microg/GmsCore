/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core.verification

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito

/**
 * Exercises the REAL MtSmsInboxRegistry suspend API and the REAL MtSmsInboxScope coroutine-context
 * element across REAL concurrent coroutine dispatch (launch on Dispatchers.Default + a
 * withContext(Dispatchers.IO) hop). Only the inbox leaf is a faithful suspending double, since
 * registering a real BroadcastReceiver needs an instrumented/Robolectric Context.
 *
 * Proves: (1) each request resolves its OWN inbox even after a dispatcher hop; (2) a late SMS
 * delivery reaches only the owning request; (3) one request disposing its scope never disposes or
 * disturbs a concurrent sibling's inbox.
 */
class MtSmsInboxDispatchTest {

    private class FakeInbox : MtSmsInboxHandle {
        private val signal = CompletableDeferred<ReceivedSms?>()
        @Volatile
        var disposed = false
            private set
        val awaiting get() = !signal.isCompleted
        override suspend fun awaitMatch(expectedBody: String, requiredSubId: Int): ReceivedSms? = signal.await()
        fun deliver(sms: ReceivedSms) { signal.complete(sms) }
        override fun dispose() {
            disposed = true
            signal.cancel(CancellationException("inbox disposed"))
        }
    }

    private fun anyContext(): Context = Mockito.mock(Context::class.java)

    @Test
    fun perRequestOwnership_lateDelivery_andDisposalDoNotLeakAcrossDispatch() = runBlocking {
        val inboxA = FakeInbox()
        val inboxB = FakeInbox()

        val aReady = CompletableDeferred<Unit>()
        val bReady = CompletableDeferred<Unit>()
        val aResult = CompletableDeferred<ReceivedSms?>()

        val jobA = launch(Dispatchers.Default + MtSmsInboxScope { _, _ -> inboxA }) {
            MtSmsInboxRegistry.prepare(anyContext(), listOf(1))
            // The per-request scope element must survive a dispatcher hop.
            val inbox = withContext(Dispatchers.IO) { MtSmsInboxRegistry.get(1) }
            assertSame("request A must resolve its OWN inbox across dispatch", inboxA, inbox)
            aReady.complete(Unit)
            try {
                aResult.complete(inbox.awaitMatch("CODE-A"))
            } finally {
                MtSmsInboxRegistry.dispose()
            }
        }

        val jobB = launch(Dispatchers.Default + MtSmsInboxScope { _, _ -> inboxB }) {
            MtSmsInboxRegistry.prepare(anyContext(), listOf(1))
            val inbox = withContext(Dispatchers.IO) { MtSmsInboxRegistry.get(1) }
            assertSame("request B must resolve its OWN inbox across dispatch", inboxB, inbox)
            bReady.complete(Unit)
            inbox.awaitMatch("CODE-B") // stays pending; must be undisturbed by A
        }

        aReady.await()
        bReady.await()

        // Late delivery, routed only to A's inbox.
        inboxA.deliver(ReceivedSms(body = "your code is CODE-A", sender = "+100"))
        assertEquals("your code is CODE-A", aResult.await()?.body)
        jobA.join()

        // A's completion + its finally { dispose() } must only tear down A's own inbox.
        assertTrue("A's own inbox should be disposed by A's cleanup", inboxA.disposed)
        assertFalse("A's disposal must not dispose a sibling request's inbox", inboxB.disposed)
        assertTrue("B must still be awaiting its own, undisturbed inbox", inboxB.awaiting)

        jobB.cancelAndJoin()
    }
}
