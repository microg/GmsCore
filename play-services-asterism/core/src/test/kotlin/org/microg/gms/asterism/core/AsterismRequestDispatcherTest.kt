/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.asterism.core

import android.os.IBinder
import android.os.RemoteException
import com.google.android.gms.asterism.internal.IAsterismCallbacks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AsterismRequestDispatcherTest {
    private class FakeCaller(
        linkThrows: Boolean = false,
        linkRuntimeThrows: Boolean = false,
        dieOnLink: Boolean = false
    ) {
        private val recipients = mutableListOf<IBinder.DeathRecipient>()
        val binder: IBinder = Mockito.mock(IBinder::class.java)
        val callbacks: IAsterismCallbacks = Mockito.mock(IAsterismCallbacks::class.java)

        init {
            Mockito.`when`(callbacks.asBinder()).thenReturn(binder)
            when {
                linkThrows -> Mockito.doThrow(RemoteException("caller already dead"))
                    .`when`(binder).linkToDeath(Mockito.any(), Mockito.anyInt())
                linkRuntimeThrows -> Mockito.doThrow(SecurityException("death tracking denied"))
                    .`when`(binder).linkToDeath(Mockito.any(), Mockito.anyInt())
                else -> Mockito.doAnswer { invocation ->
                    val recipient = invocation.getArgument<IBinder.DeathRecipient>(0)
                    recipients += recipient
                    if (dieOnLink) recipient.binderDied()
                    Unit
                }.`when`(binder).linkToDeath(Mockito.any(), Mockito.anyInt())
            }
            Mockito.doAnswer { invocation ->
                recipients.remove(invocation.getArgument<IBinder.DeathRecipient>(0))
            }.`when`(binder).unlinkToDeath(Mockito.any(), Mockito.anyInt())
        }

        fun hasDeathRecipient(): Boolean = recipients.isNotEmpty()
        fun killCaller() = recipients.toList().forEach { it.binderDied() }
    }

    @Test
    fun normalCompletion_unlinksCallerDeathRecipient() = runTest {
        val scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        try {
            val caller = FakeCaller()
            var entered = false
            val job = AsterismRequestDispatcher(scope).dispatch(caller.callbacks, "normal") {
                entered = true
            }
            advanceUntilIdle()
            assertTrue(entered)
            assertTrue(job.isCompleted)
            assertFalse(caller.hasDeathRecipient())
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun callerAlreadyDead_cancelsBeforeHandlerAndDoesNotUnlink() = runTest {
        val scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        try {
            val caller = FakeCaller(linkThrows = true)
            var entered = false
            val job = AsterismRequestDispatcher(scope).dispatch(caller.callbacks, "already-dead") {
                entered = true
                awaitCancellation()
            }
            advanceUntilIdle()
            assertTrue(job.isCancelled)
            assertFalse(entered)
            assertFalse(caller.hasDeathRecipient())
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun callerDeathDuringLink_cancelsBeforeHandlerAndUnlinks() = runTest {
        val scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        try {
            val caller = FakeCaller(dieOnLink = true)
            var entered = false
            val job = AsterismRequestDispatcher(scope).dispatch(caller.callbacks, "dies-on-link") {
                entered = true
                awaitCancellation()
            }
            advanceUntilIdle()
            assertTrue(job.isCancelled)
            assertFalse(entered)
            assertFalse(caller.hasDeathRecipient())
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun binderThatCannotTrackDeath_failsClosedBeforeHandler() = runTest {
        val scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        try {
            val caller = FakeCaller(linkRuntimeThrows = true)
            var entered = false
            val job = AsterismRequestDispatcher(scope).dispatch(caller.callbacks, "untrackable") {
                entered = true
            }
            advanceUntilIdle()
            assertTrue(job.isCancelled)
            assertFalse(entered)
            assertFalse(caller.hasDeathRecipient())
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun callerDeath_cancelsSuspendedRequestAndUnlinks() = runTest {
        val scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        try {
            val caller = FakeCaller()
            val dispatcher = AsterismRequestDispatcher(scope)
            var entered = false
            var sawCancellation = false
            val job = dispatcher.dispatch(caller.callbacks, "suspended") {
                entered = true
                try {
                    awaitCancellation()
                } catch (e: CancellationException) {
                    sawCancellation = true
                    throw e
                }
            }
            assertTrue(entered)
            assertTrue(caller.hasDeathRecipient())
            caller.killCaller()
            advanceUntilIdle()
            assertTrue(job.isCancelled)
            assertTrue(sawCancellation)
            assertFalse(caller.hasDeathRecipient())
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun callbackWrapperContainsRemoteAndRuntimeDeliveryFailures() = runTest {
        val callbacks = Mockito.mock(IAsterismCallbacks::class.java)
        Mockito.doThrow(RemoteException("binder died"))
            .`when`(callbacks).onConsentFetched(null, null)
        Mockito.doThrow(IllegalStateException("caller proxy failed"))
            .`when`(callbacks).onConsentRegistered(null, null)

        val wrapper = AsterismCallbacksWrapper(callbacks)
        wrapper.onConsentFetched(null, null)
        wrapper.onConsentRegistered(null, null)

        Mockito.verify(callbacks).onConsentFetched(null, null)
        Mockito.verify(callbacks).onConsentRegistered(null, null)
    }

    @Test
    fun callbackWrapperRethrowsCancellation() = runTest {
        val callbacks = Mockito.mock(IAsterismCallbacks::class.java)
        Mockito.doThrow(CancellationException("cancelled"))
            .`when`(callbacks).onConsentFetched(null, null)

        var rethrown = false
        try {
            AsterismCallbacksWrapper(callbacks).onConsentFetched(null, null)
        } catch (_: CancellationException) {
            rethrown = true
        }
        assertTrue(rethrown)
    }
}
