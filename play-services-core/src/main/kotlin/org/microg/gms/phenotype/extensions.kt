/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.phenotype

import com.google.android.gms.phenotype.Configuration
import com.google.android.gms.phenotype.Configurations

private val supportedLanguages = listOf(
    "bs", "pt", "ja", "ko", "fr", "it", "de", "zh", "nl",
    "iw", "he", "tr", "cs", "id", "in", "sv", "da", "no",
    "nb", "pl", "vi", "th", "fi", "uk", "ar", "el", "ru",
    "hu", "ro", "ca"
)

fun encodeSupportedLanguageList(): ByteArray {
    return supportedLanguages.flatMap { lang ->
        listOf(0x0A.toByte(), 0x02.toByte()) + lang.toByteArray(Charsets.UTF_8).toList()
    }.toByteArray()
}

fun encodeRepeatedString(entries: List<String>): ByteArray {
    val out = mutableListOf<Byte>()
    for (entry in entries) {
        val bytes = entry.toByteArray(Charsets.UTF_8)
        out.add(0x0A.toByte())
        var n = bytes.size
        while (n >= 0x80) {
            out.add(((n and 0x7F) or 0x80).toByte())
            n = n ushr 7
        }
        out.add(n.toByte())
        for (b in bytes) out.add(b)
    }
    return out.toByteArray()
}

fun configurationsResult(configurations: Array<Configuration> = emptyArray()) = Configurations().apply {
    serverToken = "unknown"
    snapshotToken = "unknown"
    version = System.currentTimeMillis() / 1000
    field4 = configurations
    field5 = false
    field6 = byteArrayOf()
}