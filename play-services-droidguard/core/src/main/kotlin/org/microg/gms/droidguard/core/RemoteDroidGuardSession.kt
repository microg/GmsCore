/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

internal class RemoteDroidGuardSession(
    private val client: RemoteDroidGuardHttpClient,
    private val requestParameters: Map<String, String>
) {
    private var remoteSessionId: String? = null

    fun begin() {
        check(remoteSessionId == null) { "Remote DroidGuard session is already open" }
        val response = client.post(null, requestParameters + ("action" to "begin"), null)
        remoteSessionId = parseSessionId(response)
            ?: throw IllegalStateException("Remote DroidGuard server did not return a session id")
    }

    fun snapshot(data: Map<Any?, Any?>?): String {
        val sessionId = checkNotNull(remoteSessionId) { "Remote DroidGuard session is not open" }
        val parameters = requestParameters + mapOf("action" to "snapshot", "sessionId" to sessionId)
        return client.post(null, parameters, RemoteDroidGuardHttpClient.encodeForm(data))
    }

    fun close() {
        val sessionId = remoteSessionId ?: return
        try {
            client.post(null, mapOf("action" to "close", "sessionId" to sessionId), null)
        } finally {
            remoteSessionId = null
        }
    }

    private fun parseSessionId(response: String): String? = response
        .split('&', '\n', '\r')
        .mapNotNull { field ->
            val separator = field.indexOf('=')
            if (separator <= 0 || decode(field.substring(0, separator)) != "sessionId") null
            else decode(field.substring(separator + 1))
        }
        .firstOrNull { it.isNotBlank() }

    private fun decode(value: String): String = java.net.URLDecoder.decode(value, Charsets.UTF_8.name())
}
