/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cast.channel

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.lang.reflect.Proxy
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** The real session and executor must complete calls rejected at either admission boundary. */
@RunWith(Parameterized::class)
class CastDeviceSessionOperationCompletionTest(private val operation: String) {
    private val completions = CopyOnWriteArrayList<Pair<String, Int>>()
    private lateinit var session: CastDeviceSession
    private lateinit var executor: ScheduledThreadPoolExecutor

    @Before
    fun setUp() {
        val callbackType = CastDeviceSession.Callbacks::class.java
        val callbacks = Proxy.newProxyInstance(callbackType.classLoader, arrayOf(callbackType)) { _, method, args ->
            if (method.name in setOf("onConnectionFailed", "onApplicationConnectionFailed", "onStopApplicationResult", "onLeaveApplicationResult")) {
                completions.add(method.name to (args!![0] as Int))
            }
            null
        } as CastDeviceSession.Callbacks
        session = CastDeviceSession("127.0.0.1", CastChannel.DEFAULT_PORT, callbacks)
        executor = field("executor").get(session) as ScheduledThreadPoolExecutor
    }

    @After
    fun tearDown() {
        session.disconnect()
        executor.shutdown()
        assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS))
    }

    @Test
    fun callAfterDisconnectReturnsOneFailure() {
        session.disconnect()
        assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS))
        invokeOperation()
        assertEquals(listOf(expectedFailure()), completions.toList())
        assertTrue((field("pendingRequests").get(session) as Map<*, *>).isEmpty())
    }

    @Test
    fun callLosingTheEnqueueRaceReturnsOneFailure() {
        executor.shutdown()
        assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS))
        executor = object : ScheduledThreadPoolExecutor(1) {
            override fun execute(command: Runnable) {
                shutdown()
                super.execute(command)
            }
        }
        field("executor").set(session, executor)
        invokeOperation()
        assertEquals(listOf(expectedFailure()), completions.toList())
        assertTrue((field("pendingRequests").get(session) as Map<*, *>).isEmpty())
    }

    private fun invokeOperation() {
        when (operation) {
            "connect" -> session.connect()
            "launch" -> session.launchApplication("app-id", true, null)
            "join" -> session.joinApplication("app-id", "session-id")
            "stop" -> session.stopApplication("session-id")
            "leave" -> session.leaveApplication()
        }
    }

    private fun expectedFailure(): Pair<String, Int> = when (operation) {
        "connect" -> "onConnectionFailed" to CastDeviceSession.STATUS_NETWORK_ERROR
        "launch", "join" -> "onApplicationConnectionFailed" to CastDeviceSession.STATUS_APPLICATION_NOT_RUNNING
        "stop" -> "onStopApplicationResult" to CastDeviceSession.STATUS_APPLICATION_NOT_RUNNING
        else -> "onLeaveApplicationResult" to CastDeviceSession.STATUS_APPLICATION_NOT_RUNNING
    }

    private fun field(name: String) = CastDeviceSession::class.java.getDeclaredField(name).apply { isAccessible = true }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun operations() = listOf(arrayOf("connect"), arrayOf("launch"), arrayOf("join"), arrayOf("stop"), arrayOf("leave"))
    }
}

/** Preserve completion of already-admitted requests when the real channel closes. */
class CastDeviceSessionPendingCloseControlTest {
    @Test
    fun disconnectCompletesAdmittedLaunchJoinAndStopExactlyOnce() {
        val completions = CopyOnWriteArrayList<Pair<String, Int>>()
        val callbackType = CastDeviceSession.Callbacks::class.java
        val callbacks = Proxy.newProxyInstance(callbackType.classLoader, arrayOf(callbackType)) { _, method, args ->
            if (method.name in setOf("onApplicationConnectionFailed", "onStopApplicationResult")) {
                completions.add(method.name to (args!![0] as Int))
            }
            null
        } as CastDeviceSession.Callbacks
        val session = CastDeviceSession("127.0.0.1", CastChannel.DEFAULT_PORT, callbacks)
        val executorField = CastDeviceSession::class.java.getDeclaredField("executor").apply { isAccessible = true }
        val executor = executorField.get(session) as ScheduledThreadPoolExecutor
        val channel = CastChannel("127.0.0.1", CastChannel.DEFAULT_PORT, session, "sender-under-test")
        CastChannel::class.java.getDeclaredField("output").apply { isAccessible = true }.set(channel, DataOutputStream(ByteArrayOutputStream()))
        CastDeviceSession::class.java.getDeclaredField("channel").apply { isAccessible = true }.set(session, channel)
        try {
            session.launchApplication("app-id", true, null)
            session.joinApplication("app-id", "session-id")
            session.stopApplication("session-id")
            executor.submit {}.get(2, TimeUnit.SECONDS)
            session.disconnect()
            assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS))
            assertEquals(2, completions.count { it == ("onApplicationConnectionFailed" to CastDeviceSession.STATUS_TIMEOUT) })
            assertEquals(1, completions.count { it == ("onStopApplicationResult" to CastDeviceSession.STATUS_TIMEOUT) })
            assertEquals(3, completions.size)
            val pending = CastDeviceSession::class.java.getDeclaredField("pendingRequests").apply { isAccessible = true }.get(session) as Map<*, *>
            assertTrue(pending.isEmpty())
            session.onClosed(null)
            assertEquals(3, completions.size)
        } finally {
            session.disconnect()
            executor.shutdown()
            assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS))
        }
    }

    @Test
    fun callbackFailureDoesNotInterruptRemoteCloseCleanup() {
        val completions = CopyOnWriteArrayList<Pair<String, Int>>()
        val pendingSizes = CopyOnWriteArrayList<Int>()
        val pendingField = CastDeviceSession::class.java.getDeclaredField("pendingRequests").apply { isAccessible = true }
        lateinit var session: CastDeviceSession
        var failNextCompletion = true
        val callbackType = CastDeviceSession.Callbacks::class.java
        val callbacks = Proxy.newProxyInstance(callbackType.classLoader, arrayOf(callbackType)) { _, method, args ->
            if (method.name in setOf("onApplicationConnectionFailed", "onStopApplicationResult", "onDisconnected")) {
                completions.add(method.name to (args!![0] as Int))
                pendingSizes.add((pendingField.get(session) as Map<*, *>).size)
                if (failNextCompletion) {
                    failNextCompletion = false
                    throw IllegalStateException("Client callback failed")
                }
            }
            null
        } as CastDeviceSession.Callbacks
        session = CastDeviceSession("127.0.0.1", CastChannel.DEFAULT_PORT, callbacks)
        val executor = CastDeviceSession::class.java.getDeclaredField("executor").apply { isAccessible = true }
            .get(session) as ScheduledThreadPoolExecutor
        val channel = CastChannel("127.0.0.1", CastChannel.DEFAULT_PORT, session, "sender-under-test")
        CastChannel::class.java.getDeclaredField("output").apply { isAccessible = true }
            .set(channel, DataOutputStream(ByteArrayOutputStream()))
        CastDeviceSession::class.java.getDeclaredField("channel").apply { isAccessible = true }.set(session, channel)
        try {
            session.launchApplication("app-id", true, null)
            session.joinApplication("app-id", "session-id")
            session.stopApplication("session-id")
            executor.submit {}.get(2, TimeUnit.SECONDS)
            val pending = (pendingField.get(session) as Map<*, *>).values.toList()
            assertEquals(3, pending.size)
            val timeouts = pending.map { request ->
                request!!.javaClass.getDeclaredField("timeout").apply { isAccessible = true }
                    .get(request) as java.util.concurrent.ScheduledFuture<*>
            }
            session.onClosed(IOException("Receiver disconnected"))
            executor.submit {}.get(2, TimeUnit.SECONDS)
            assertEquals(2, completions.count { it == ("onApplicationConnectionFailed" to CastDeviceSession.STATUS_TIMEOUT) })
            assertEquals(1, completions.count { it == ("onStopApplicationResult" to CastDeviceSession.STATUS_TIMEOUT) })
            assertEquals(1, completions.count { it == ("onDisconnected" to CastDeviceSession.STATUS_NETWORK_ERROR) })
            assertEquals(4, completions.size)
            assertEquals(listOf(0, 0, 0, 0), pendingSizes.toList())
            assertTrue(timeouts.all { it.isCancelled })
            assertTrue((pendingField.get(session) as Map<*, *>).isEmpty())
        } finally {
            session.disconnect()
            executor.shutdown()
            assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS))
        }
    }
}
