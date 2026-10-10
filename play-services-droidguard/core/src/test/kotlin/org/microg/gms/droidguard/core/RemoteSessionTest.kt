/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.droidguard.core

import org.junit.Assert.*
import org.junit.Test

class RemoteSessionTest {
    @Test fun lifecycleAndReinitialization() {
        val calls = mutableListOf<Pair<String, Map<String, String>>>()
        var sequence = 0
        val session = RemoteSession { operation, parameters ->
            calls += operation to parameters
            if (operation == "init") "session_${++sequence}" else "result"
        }
        session.init(mapOf("flow" to "pia_attest_e1", "request-clientVersion" to "1"))
        session.snapshot(mapOf("data-step" to "first"))
        session.snapshot(mapOf("data-step" to "second"))
        session.init(mapOf("flow" to "other"))
        session.close()
        session.close()
        assertEquals(listOf("init", "snapshot", "snapshot", "close", "init", "close"), calls.map { it.first })
        assertEquals("session_1", calls[1].second["session"])
        assertEquals("session_1", calls[2].second["session"])
        assertEquals("second", calls[2].second["data-step"])
        assertEquals("session_2", calls.last().second["session"])
        assertThrows(IllegalStateException::class.java) { session.snapshot(emptyMap()) }
    }

    @Test fun failureDoesNotRestartOrDowngrade() {
        val calls = mutableListOf<String>()
        val session = RemoteSession { operation, _ ->
            calls += operation
            if (operation == "init") "id" else throw java.io.IOException("offline")
        }
        session.init(emptyMap())
        assertThrows(java.io.IOException::class.java) { session.snapshot(emptyMap()) }
        assertThrows(java.io.IOException::class.java) { session.close() }
        session.close()
        assertThrows(IllegalStateException::class.java) { session.snapshot(emptyMap()) }
        assertEquals(listOf("init", "snapshot", "close"), calls)
    }

    @Test fun invalidInitCannotBeUsed() {
        val session = RemoteSession { _, _ -> "not an id\n" }
        assertThrows(IllegalArgumentException::class.java) { session.init(emptyMap()) }
        assertThrows(IllegalStateException::class.java) { session.snapshot(emptyMap()) }
    }
}
