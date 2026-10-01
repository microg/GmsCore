/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal class RemoteDroidGuardHttpClient(
    private val baseUrl: String,
    private val timeoutMillis: Int,
    private val openConnection: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection }
) {
    fun post(path: String?, query: Map<String, String>, body: ByteArray?): String {
        val connection = openConnection(URL(buildUrl(baseUrl, path, query)))
        try {
            connection.connectTimeout = timeoutMillis.coerceAtLeast(1)
            connection.readTimeout = timeoutMillis.coerceAtLeast(1)
            connection.requestMethod = "POST"
            connection.doInput = true
            connection.doOutput = body != null
            if (body != null) {
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                connection.outputStream.use { it.write(body) }
            }
            val status = connection.responseCode
            if (status !in 200..299) throw IOException("Remote DroidGuard server returned HTTP $status")
            return connection.inputStream.use { it.readBytes().toString(StandardCharsets.UTF_8) }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        fun encodeForm(fields: Map<Any?, Any?>?): ByteArray {
            val encoded = fields.orEmpty().entries.mapNotNull { entry ->
                val key = entry.key?.toString() ?: return@mapNotNull null
                val value = entry.value?.toString() ?: return@mapNotNull null
                "${encodeComponent(key)}=${encodeComponent(value)}"
            }.joinToString("&")
            return encoded.toByteArray(StandardCharsets.UTF_8)
        }

        private fun buildUrl(baseUrl: String, path: String?, query: Map<String, String>): String {
            val base = URI(baseUrl)
            require(base.scheme == "http" || base.scheme == "https") { "Remote DroidGuard URL must use HTTP or HTTPS" }
            require(base.rawAuthority != null && base.rawFragment == null) { "Remote DroidGuard URL must contain a host and no fragment" }

            val basePath = base.rawPath.orEmpty().trimEnd('/')
            val fullPath = if (path == null) basePath else "$basePath/${encodeComponent(path)}"
            val queryParts = mutableListOf<String>()
            base.rawQuery?.takeIf { it.isNotEmpty() }?.let(queryParts::add)
            query.toSortedMap().forEach { (key, value) ->
                queryParts += "${encodeComponent(key)}=${encodeComponent(value)}"
            }
            val fullQuery = if (queryParts.isEmpty()) "" else "?${queryParts.joinToString("&")}" 
            return "${base.scheme}://${base.rawAuthority}$fullPath$fullQuery"
        }

        private fun encodeComponent(value: String): String =
            URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
    }
}
