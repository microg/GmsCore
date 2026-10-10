/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.droidguard.core

/** One remote VM per handle. Transport failures must never restart a multi-step flow. */
internal class RemoteSession(private val post: (String, Map<String, String>) -> String) {
    private var id: String? = null

    @Synchronized
    fun init(parameters: Map<String, String>) {
        close()
        val newId = post("init", parameters)
        require(newId.matches(Regex("[A-Za-z0-9_-]{1,128}"))) { "Invalid remote session ID" }
        id = newId
    }

    @Synchronized
    fun snapshot(parameters: Map<String, String>): String {
        val sessionId = checkNotNull(id) { "Remote handle is not initialized" }
        return post("snapshot", parameters + ("session" to sessionId))
    }

    @Synchronized
    fun close() {
        val sessionId = id ?: return
        id = null
        post("close", mapOf("session" to sessionId))
    }
}
