/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Loopback retained-session backend for the remote DroidGuard handle lifecycle.
 *
 * Speaks `action=begin|snapshot|close` with a `sessionId`. Snapshot bodies are echoed with a
 * session-local sequence number so interop can observe reuse. This is not a single-attest
 * server and does not produce Play Integrity, Dott, or device results.
 */
internal class RemoteDroidGuardSessionServer : AutoCloseable {
    private val nextSession = AtomicInteger(1)
    private val sessions = ConcurrentHashMap<String, Session>()
    val actions = Collections.synchronizedList(mutableListOf<String>())
    val snapshotBodies = Collections.synchronizedList(mutableListOf<String>())
    val sessionIds = Collections.synchronizedList(mutableListOf<String>())
    private val server = HttpServer.create(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0)

    val url: String
        get() = "http://127.0.0.1:${server.address.port}/droidguard"

    init {
        server.createContext("/droidguard", ::handle)
        server.executor = null
        server.start()
    }

    fun session(id: String): Session? = sessions[id]

    override fun close() {
        server.stop(0)
    }

    private fun handle(exchange: HttpExchange) {
        val query = query(exchange.requestURI)
        val action = query["action"]
        val body = exchange.requestBody.use { it.readBytes().toString(StandardCharsets.UTF_8) }
        try {
            when (action) {
                "begin" -> {
                    actions.add("begin")
                    val id = "remote/${nextSession.getAndIncrement()}"
                    sessions[id] = Session(id)
                    sessionIds.add(id)
                    respond(exchange, 200, "sessionId=${encode(id)}")
                }
                "snapshot" -> {
                    actions.add("snapshot")
                    val session = openSession(query["sessionId"]) ?: return respond(exchange, 400, "error=session")
                    snapshotBodies.add(body)
                    sessionIds.add(session.id)
                    respond(exchange, 200, session.echo(body))
                }
                "close" -> {
                    actions.add("close")
                    val session = openSession(query["sessionId"]) ?: return respond(exchange, 400, "error=session")
                    session.closed = true
                    sessionIds.add(session.id)
                    respond(exchange, 200, "status=ok")
                }
                else -> respond(exchange, 400, "error=action")
            }
        } catch (e: Exception) {
            respond(exchange, 500, "error=${encode(e.message ?: e.javaClass.simpleName)}")
        }
    }

    private fun openSession(sessionId: String?): Session? {
        val session = sessions[sessionId] ?: return null
        return if (session.closed) null else session
    }

    private fun query(uri: URI): Map<String, String> {
        val raw = uri.rawQuery ?: return emptyMap()
        return raw.split('&').mapNotNull { part ->
            val separator = part.indexOf('=')
            if (separator <= 0) null
            else decode(part.substring(0, separator)) to decode(part.substring(separator + 1))
        }.toMap()
    }

    private fun respond(exchange: HttpExchange, status: Int, body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun decode(value: String): String =
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())

    class Session(val id: String) {
        @Volatile
        var closed = false
        private var snapshots = 0

        @Synchronized
        fun echo(body: String): String {
            snapshots += 1
            val payload = "session=${id}&n=${snapshots}&${body}"
            return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray(StandardCharsets.UTF_8))
        }
    }
}
