/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text.model

import android.graphics.PointF

data class OCRBox(
    val points: List<PointF>,
) {
    init {
        require(points.size == 4) { "OCRBox must have exactly 4 points, got ${points.size}" }
    }
}

data class OCRResult(
    val box: OCRBox,
    val text: String,
    val confidence: Float,
    val words: List<OCRWord> = emptyList(),
    val readingStartCorner: Int = 0,
)

data class OCRWord(val box: OCRBox, val text: String)

internal fun List<OCRResult>.mapToSource(
    scaleX: Float,
    scaleY: Float,
    offsetX: Int,
    offsetY: Int,
): List<OCRResult> {
    fun mapBox(box: OCRBox) = OCRBox(box.points.map {
        PointF(it.x * scaleX + offsetX, it.y * scaleY + offsetY)
    })

    val needsTransform = scaleX != 1f || scaleY != 1f || offsetX != 0 || offsetY != 0
    return mapNotNull { result ->
        if (result.text.isBlank()) return@mapNotNull null
        val confidence = if (result.confidence.isFinite()) result.confidence.coerceIn(0f, 1f) else 0f
        when {
            needsTransform -> result.copy(
                box = mapBox(result.box),
                confidence = confidence,
                words = result.words.map { it.copy(box = mapBox(it.box)) },
            )
            confidence != result.confidence -> result.copy(confidence = confidence)
            else -> result
        }
    }
}
