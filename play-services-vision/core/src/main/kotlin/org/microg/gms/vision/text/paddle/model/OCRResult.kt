/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text.paddle.model

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
)

data class OCRWord(val box: OCRBox, val text: String)
