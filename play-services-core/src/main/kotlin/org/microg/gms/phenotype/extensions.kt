/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.phenotype

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