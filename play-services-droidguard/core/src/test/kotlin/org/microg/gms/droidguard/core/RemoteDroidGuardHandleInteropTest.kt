/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import com.google.android.gms.droidguard.DroidGuardClient
import com.google.android.gms.droidguard.DroidGuardHandle
import com.google.android.gms.tasks.Tasks
import com.sun.net.httpserver.HttpServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit

class RemoteDroidGuardHandleInteropTest {
    @Test
    fun initReturnsOpenedHandleThatReusesOneSessionAcrossSnapshotsThenClose() {
        RemoteDroidGuardSessionServer().use { server ->
            val client: DroidGuardClient = RemoteDroidGuardHandleClient(server.url, "com.example.app", 2_000)
            val handle: DroidGuardHandle = Tasks.await(client.init("play_integrity", null), 5, TimeUnit.SECONDS)

            assertTrue(handle.isOpened())
            val first = decode(handle.snapshot(mapOf("rpc" to "challenge")))
            val second = decode(handle.snapshot(mapOf("rpc" to "sign")))
            assertTrue(handle.isOpened())

            handle.close()
            assertFalse(handle.isOpened())
            try {
                handle.snapshot(mapOf("rpc" to "late"))
                fail("A closed handle must refuse further snapshots")
            } catch (e: IllegalStateException) {
                assertTrue(e.message.orEmpty().contains("closed"))
            }

            assertEquals(listOf("begin", "snapshot", "snapshot", "close"), server.actions.toList())
            assertEquals(listOf("rpc=challenge", "rpc=sign"), server.snapshotBodies.toList())
            assertEquals(listOf("remote/1", "remote/1", "remote/1", "remote/1"), server.sessionIds.toList())
            assertTrue(server.session("remote/1")!!.closed)
            assertEquals("session=remote/1&n=1&rpc=challenge", first)
            assertEquals("session=remote/1&n=2&rpc=sign", second)
        }
    }

    @Test
    fun eachInitTaskOpensADistinctServerSession() {
        RemoteDroidGuardSessionServer().use { server ->
            val client: DroidGuardClient = RemoteDroidGuardHandleClient(server.url, "com.example.app", 2_000)
            val first = Tasks.await(client.init("play_integrity", null), 5, TimeUnit.SECONDS)
            val second = Tasks.await(client.init("play_integrity", null), 5, TimeUnit.SECONDS)
            try {
                assertTrue(first.isOpened())
                assertTrue(second.isOpened())
                assertEquals("session=remote/1&n=1&rpc=challenge", decode(first.snapshot(mapOf("rpc" to "challenge"))))
                assertEquals("session=remote/2&n=1&rpc=sign", decode(second.snapshot(mapOf("rpc" to "sign"))))
            } finally {
                first.close()
                second.close()
            }
            assertFalse(first.isOpened())
            assertFalse(second.isOpened())
            assertEquals(listOf("begin", "begin", "snapshot", "snapshot", "close", "close"), server.actions.toList())
            assertEquals(setOf("remote/1", "remote/2"), server.sessionIds.toSet())
        }
    }

    @Test
    fun initTaskFailsAgainstASingleAttestServerThatHasNoSession() {
        SingleAttestServer().use { server ->
            val client: DroidGuardClient = RemoteDroidGuardHandleClient(server.url, "com.example.app", 2_000)
            try {
                Tasks.await(client.init("play_integrity", null), 5, TimeUnit.SECONDS)
                fail("A single-attest server must not be treated as a session backend")
            } catch (e: Exception) {
                assertTrue(e.message.orEmpty().contains("session id") || e.cause?.message.orEmpty().contains("session id"))
            }
        }
    }

    @Test
    fun rejectingExecutorReturnsFailedTasksWithoutOpeningASession() {
        RemoteDroidGuardSessionServer().use { server ->
            val rejected = RejectedExecutionException("executor stopped")
            val executor = Executor { throw rejected }
            val client = RemoteDroidGuardHandleClient(server.url, "com.example.app", 2_000, executor)

            val tasks = listOf(
                client.init("play_integrity", null),
                client.getResults("play_integrity", emptyMap(), null)
            )
            for (task in tasks) {
                assertTrue(task.isComplete)
                assertFalse(task.isSuccessful)
                assertSame(rejected, task.exception)
            }
            assertTrue(server.actions.isEmpty())
        }
    }

    @Test
    fun getResultsCloseFailureDoesNotEscapeAfterSuccessfulSnapshot() {
        RemoteDroidGuardSessionServer().use { server ->
            var worker: Runnable? = null
            val executor = Executor { work -> worker = work }
            val client = RemoteDroidGuardHandleClient(server.url, "com.example.app", 2_000, executor)
            val result = client.getResults("play_integrity", mapOf("rpc" to "challenge"), null)
            result.addOnCompleteListener(Executor { it.run() }) {
                server.close()
            }
            var escaped: Throwable? = null

            try {
                worker!!.run()
            } catch (t: Throwable) {
                escaped = t
            }

            assertTrue(result.isComplete)
            assertTrue(result.isSuccessful)
            assertEquals("session=remote/1&n=1&rpc=challenge", decode(result.result))
            assertEquals(null, escaped)
            assertEquals(listOf("begin", "snapshot"), server.actions.toList())
        }
    }

    @Test
    fun getResultsUsesOneAcceptedWorkerAndClosesTheSessionAfterCompletion() {
        RemoteDroidGuardSessionServer().use { server ->
            val rejected = RejectedExecutionException("executor stopped after init")
            var submissions = 0
            var initialization: Runnable? = null
            val executor = Executor { work ->
                submissions += 1
                if (submissions == 1) initialization = work else throw rejected
            }
            val client = RemoteDroidGuardHandleClient(server.url, "com.example.app", 2_000, executor)
            val result = client.getResults("play_integrity", mapOf("rpc" to "challenge"), null)
            var closedAtCompletion: Boolean? = null
            result.addOnCompleteListener(Executor { it.run() }) {
                closedAtCompletion = server.session("remote/1")?.closed
            }
            assertFalse(result.isComplete)

            initialization!!.run()

            assertEquals(1, submissions)
            assertTrue(result.isComplete)
            assertTrue(result.isSuccessful)
            assertEquals("session=remote/1&n=1&rpc=challenge", decode(result.result))
            assertEquals(false, closedAtCompletion)
            assertEquals(listOf("begin", "snapshot", "close"), server.actions.toList())
            assertEquals(listOf("rpc=challenge"), server.snapshotBodies.toList())
            assertTrue(server.session("remote/1")!!.closed)
        }
    }

    private fun decode(value: String): String =
        String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)

    /**
     * Length-style single POST backend: one body in, one base64 blob out, no session id.
     * Retained-handle init must not adopt this as a session.
     */
    private class SingleAttestServer : AutoCloseable {
        private val server = HttpServer.create(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0)
        val url: String
            get() = "http://127.0.0.1:${server.address.port}/attest"

        init {
            server.createContext("/attest") { exchange ->
                exchange.requestBody.use { it.readBytes() }
                val body = "dGVzdA".toByteArray(StandardCharsets.UTF_8)
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            server.start()
        }

        override fun close() {
            server.stop(0)
        }
    }
}
