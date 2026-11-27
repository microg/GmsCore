/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.sharing

import java.io.File
import java.nio.charset.StandardCharsets

internal object NearbyFileNames {
    private val unsafeCharacters = Regex("[<>:\"/\\\\|?*\\p{Cntrl}]")
    private val trailingDotsAndSpaces = Regex("[. ]+$")
    private const val MAX_NAME_BYTES = 220

    fun sanitize(requested: String?, fallback: String): String {
        val leaf = requested
            ?.replace('\u0000', ' ')
            ?.substringAfterLast('/')
            ?.substringAfterLast('\\')
            ?.replace(unsafeCharacters, "_")
            ?.replace(trailingDotsAndSpaces, "")
            ?.trim()

        if (leaf.isNullOrEmpty() || leaf == "." || leaf == "..") return fallback
        if (leaf.toByteArray(StandardCharsets.UTF_8).size <= MAX_NAME_BYTES) return leaf

        val dot = leaf.lastIndexOf('.')
        val extension = if (dot > 0 && leaf.length - dot <= 16) leaf.substring(dot) else ""
        val extensionBytes = extension.toByteArray(StandardCharsets.UTF_8).size
        val stem = if (extension.isEmpty()) leaf else leaf.substring(0, dot)
        return truncateUtf8(stem, MAX_NAME_BYTES - extensionBytes).trimEnd() + extension
    }

    fun unique(directory: File, name: String): File {
        val first = File(directory, name)
        if (!first.exists()) return first

        val dot = name.lastIndexOf('.')
        val hasExtension = dot > 0
        val stem = if (hasExtension) name.substring(0, dot) else name
        val extension = if (hasExtension) name.substring(dot) else ""
        var suffix = 1
        while (true) {
            val candidate = File(directory, "$stem ($suffix)$extension")
            if (!candidate.exists()) return candidate
            suffix++
        }
    }

    private fun truncateUtf8(value: String, maxBytes: Int): String {
        val out = StringBuilder()
        var bytes = 0
        var index = 0
        while (index < value.length) {
            val codePoint = value.codePointAt(index)
            val part = String(Character.toChars(codePoint))
            val partBytes = part.toByteArray(StandardCharsets.UTF_8).size
            if (bytes + partBytes > maxBytes) break
            out.append(part)
            bytes += partBytes
            index += Character.charCount(codePoint)
        }
        return out.toString()
    }
}
