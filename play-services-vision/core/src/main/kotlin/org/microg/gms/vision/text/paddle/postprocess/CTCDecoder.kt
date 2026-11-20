/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text.paddle.postprocess

object CTCDecoder {
    private const val BLANK_IDX = 0

    fun decode(output: FloatArray, shape: LongArray, characterList: List<String>): Pair<String, Float> {
        require(shape.size == 3 && shape[0] == 1L && shape.all { it > 0 }) { "Invalid OCR recognition output shape" }
        require(shape[2] == characterList.size.toLong() + 1) { "OCR model and character dictionary do not match" }
        var expectedOutputSize = 1L
        for (dimension in shape) {
            expectedOutputSize = Math.multiplyExact(expectedOutputSize, dimension)
        }
        require(expectedOutputSize == output.size.toLong()) {
            "OCR recognition output is truncated"
        }
        val timeSteps = shape[1].toInt()
        val numClasses = shape[2].toInt()

        val sb = StringBuilder()
        var prevIdx = -1
        var confidenceSum = 0.0
        var characterCount = 0
        for (t in 0 until timeSteps) {
            val offset = t * numClasses
            var maxIdx = 0
            var maxVal = output[offset]
            for (c in 1 until numClasses) {
                val v = output[offset + c]
                if (v > maxVal) {
                    maxVal = v
                    maxIdx = c
                }
            }
            if (maxIdx != BLANK_IDX && maxIdx != prevIdx) {
                sb.append(characterList[maxIdx - 1])
                confidenceSum += maxVal.toDouble()
                characterCount++
            }
            prevIdx = maxIdx
        }
        val confidence = if (characterCount > 0) (confidenceSum / characterCount).toFloat() else 0f
        return Pair(sb.toString(), confidence)
    }
}
