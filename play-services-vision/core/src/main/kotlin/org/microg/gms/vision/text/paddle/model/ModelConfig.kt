/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text.paddle.model

import android.content.Context

enum class RecognitionModel(val assetDirectory: String) {
    LATIN("ocr/latin_ppocrv5_mobile/rec"),
    CJK("ocr/ppocrv6_small/rec"),
    KOREAN("ocr/korean_ppocrv5_mobile/rec"),
    DEVANAGARI("ocr/devanagari_ppocrv5_mobile/rec"),
}

object ModelConfig {
    fun loadCharacterList(context: Context, assetPath: String): List<String> {
        return try {
            val content = context.assets.open(assetPath).bufferedReader().use { it.readText() }
            val characters = extractCharacterDict(content)
            if (characters.lastOrNull() != " ") {
                characters.add(" ")
            }
            characters
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse OCR character dictionary: $assetPath", e)
        }
    }

    private fun extractCharacterDict(content: String): MutableList<String> {
        val normalizedContent = content.replace("\r\n", "\n").replace('\r', '\n')
        val lines = normalizedContent.lines()
        val postProcessLine = lines.indexOfFirst { it.trim() == "PostProcess:" }
        require(postProcessLine >= 0) { "Missing PostProcess" }

        val postProcessIndent = leadingSpaces(lines[postProcessLine])
        val characterDictLine = findCharacterDictLine(lines, postProcessLine + 1, postProcessIndent)
        require(characterDictLine >= 0) { "Missing character_dict" }

        val characterDictIndent = leadingSpaces(lines[characterDictLine])
        val characters = mutableListOf<String>()
        var lineIndex = characterDictLine + 1
        while (lineIndex < lines.size) {
            val line = lines[lineIndex]
            val indent = leadingSpaces(line)
            val content = line.substring(indent)
            val keyLikeContent = content.trim()
            if (keyLikeContent.isEmpty() || keyLikeContent.startsWith("#")) {
                lineIndex++
                continue
            }

            if (!content.startsWith("-")) {
                if (indent <= characterDictIndent) {
                    break
                }
                lineIndex++
                continue
            }

            characters.add(parseYamlListScalar(content.substring(1)))
            lineIndex++
        }

        require(characters.isNotEmpty()) { "Empty character_dict" }
        return characters
    }

    private fun findCharacterDictLine(
        lines: List<String>,
        startLine: Int,
        postProcessIndent: Int,
    ): Int {
        var lineIndex = startLine
        while (lineIndex < lines.size) {
            val line = lines[lineIndex]
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                lineIndex++
                continue
            }
            val indent = leadingSpaces(line)
            if (indent <= postProcessIndent) {
                break
            }
            if (trimmed == "character_dict:") {
                return lineIndex
            }
            lineIndex++
        }
        return -1
    }

    private fun parseYamlListScalar(rawValue: String): String {
        val value = rawValue.dropWhile { it == ' ' }
        if (value.length >= 2 && value.first() == '\'' && value.last() == '\'') {
            return value.substring(1, value.length - 1).replace("''", "'")
        }
        if (value.length >= 2 && value.first() == '"' && value.last() == '"') {
            return value.substring(1, value.length - 1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
        }
        return value
    }

    private fun leadingSpaces(line: String): Int {
        val firstNonSpaceIndex = line.indexOfFirst { it != ' ' }
        return if (firstNonSpaceIndex < 0) line.length else firstNonSpaceIndex
    }
}
