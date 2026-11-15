/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core.verification

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.ArrayDeque
import kotlin.coroutines.CoroutineContext

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
@Config(sdk = [34])
class MtSmsInboxReplayTest {

    private fun newInbox() = MtSmsInbox(
        ApplicationProvider.getApplicationContext<Context>(),
        subId = 1
    )

    @Test
    fun unrelatedSmsBacklogIsBoundedWhileRecentMessagesRemainMatchable() = runTest {
        val inbox = newInbox()
        try {
            val messages = (0..80).map { index ->
                ReceivedSms(body = "unrelated-$index", sender = "+100")
            }
            inbox.onReceivedMessages(messages)

            assertNull(
                "an old unrelated SMS should have been evicted instead of accumulating forever",
                withTimeoutOrNull(1L) { inbox.awaitMatch("unrelated-0") }
            )
            assertEquals(messages.last(), inbox.awaitMatch("unrelated-80"))
        } finally {
            inbox.dispose()
        }
    }

    @Test
    fun bufferedMatchIsConsumedBeforeTheNextChallengeRound() = runTest {
        val inbox = newInbox()
        try {
            val sms = ReceivedSms(body = "verification challenge-123 complete", sender = "+100")
            inbox.onReceivedMessages(listOf(sms))

            assertEquals(sms, inbox.awaitMatch("challenge-123"))
            assertNull(
                "a later round must not reuse the SMS already consumed by this inbox",
                withTimeoutOrNull(1L) { inbox.awaitMatch("challenge-123") }
            )
        } finally {
            inbox.dispose()
        }
    }

    @Test
    fun liveMatchIsNotRetainedInTheBufferForTheNextRound() = runTest {
        val inbox = newInbox()
        try {
            val waiter = async { inbox.awaitMatch("challenge-123") }
            runCurrent() // Let awaitMatch register its pending continuation.

            val sms = ReceivedSms(body = "verification challenge-123 complete", sender = "+100")
            inbox.onReceivedMessages(listOf(sms))
            assertEquals(sms, waiter.await())

            assertNull(
                "a live delivery that satisfied a waiter must not also remain buffered",
                withTimeoutOrNull(1L) { inbox.awaitMatch("challenge-123") }
            )
        } finally {
            inbox.dispose()
        }
    }

    @Test
    fun cancellationBeforeDispatchReturnsTheSmsToTheBuffer() = runTest {
        val inbox = newInbox()
        val dispatcher = ManualDispatcher()
        val requestJob = Job()
        val waiter = CoroutineScope(requestJob + dispatcher).async {
            inbox.awaitMatch("challenge-123")
        }

        try {
            // Start the waiter and leave it suspended in awaitMatch.
            assertTrue(dispatcher.runNext())

            // Matching reserves the continuation and queues its dispatch, but the waiter has not
            // run yet. Cancelling here exercises CancellableContinuation's prompt-cancellation path.
            val sms = ReceivedSms(body = "verification challenge-123 complete", sender = "+100")
            inbox.onReceivedMessages(listOf(sms))
            assertFalse(waiter.isCompleted)
            requestJob.cancel()
            dispatcher.drain()

            assertTrue(waiter.isCancelled)
            assertEquals(
                "an SMS reserved for a waiter cancelled before dispatch must be retained",
                sms,
                withTimeoutOrNull(1L) { inbox.awaitMatch("challenge-123") }
            )
        } finally {
            requestJob.cancel()
            dispatcher.drain()
            inbox.dispose()
        }
    }

    @Test
    fun sharedCatchAllInboxFiltersBySubscriptionAndConsumesEachSmsOnce() = runTest {
        val inbox = MtSmsInbox(
            ApplicationProvider.getApplicationContext<Context>(),
            subId = -1
        )
        try {
            val simTwo = ReceivedSms(
                body = "verification challenge-123 complete",
                sender = "+200",
                subscriptionId = 2
            )
            val simOne = ReceivedSms(
                body = "verification challenge-123 complete",
                sender = "+100",
                subscriptionId = 1
            )
            inbox.onReceivedMessages(listOf(simTwo, simOne))

            assertEquals(simOne, inbox.awaitMatch("challenge-123", requiredSubId = 1))
            assertEquals(simTwo, inbox.awaitMatch("challenge-123", requiredSubId = -1))
            assertNull(
                "consumed SMS evidence must not be reusable through the catch-all path",
                withTimeoutOrNull(1L) {
                    inbox.awaitMatch("challenge-123", requiredSubId = -1)
                }
            )
        } finally {
            inbox.dispose()
        }
    }

    @Test
    fun disposingRealInboxCancelsSuspendedWaiterAndRejectsLaterWaiters() = runTest {
        val inbox = newInbox()
        try {
            val pattern = "VERIFY_OTP_999888"
            val pendingWaiter = async { inbox.awaitMatch(pattern) }
            runCurrent()
            assertTrue("waiter should be suspended awaiting an SMS", pendingWaiter.isActive)

            inbox.dispose()
            runCurrent()
            assertTrue("dispose must cancel a real MtSmsInbox waiter", pendingWaiter.isCancelled)

            val cancellation = try {
                pendingWaiter.await()
                null
            } catch (e: CancellationException) {
                e
            }
            assertEquals("MT SMS inbox disposed", cancellation?.message)

            val afterDispose = async { inbox.awaitMatch(pattern) }
            runCurrent()
            assertTrue("a disposed inbox must reject later waiters", afterDispose.isCancelled)
            val laterCancellation = try {
                afterDispose.await()
                null
            } catch (e: CancellationException) {
                e
            }
            assertEquals("MT SMS inbox disposed", laterCancellation?.message)
        } finally {
            inbox.dispose()
        }
    }

    private class ManualDispatcher : CoroutineDispatcher() {
        private val queued = ArrayDeque<Runnable>()

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            queued.addLast(block)
        }

        fun runNext(): Boolean {
            val next = queued.pollFirst() ?: return false
            next.run()
            return true
        }

        fun drain() {
            while (runNext()) Unit
        }
    }
}
