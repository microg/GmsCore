/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text

import android.graphics.Point
import com.google.android.gms.vision.text.internal.client.BoundingBoxParcel
import com.google.android.gms.vision.text.internal.client.LineBoxParcel
import com.google.android.gms.vision.text.internal.client.WordBoxParcel
import org.microg.gms.vision.text.paddle.model.OCRBox
import org.microg.gms.vision.text.paddle.model.OCRResult
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

internal fun List<OCRResult>.toLegacyText(): Array<LineBoxParcel> = buildList(size) {
    for (block in this@toLegacyText.groupTextLines()) {
        for (index in block) {
            val result = this@toLegacyText[index]
            val box = result.box.toLegacyBox()
            val words = if (result.words.isEmpty()) {
                arrayOf(WordBoxParcel(emptyArray(), box, box, result.text, result.confidence, "und", false))
            } else {
                result.words.map { word ->
                    val wordBox = word.box.toLegacyBox()
                    WordBoxParcel(emptyArray(), wordBox, wordBox, word.text, result.confidence, "und", false)
                }.toTypedArray()
            }
            add(LineBoxParcel(words, box, box, box, result.text, result.confidence, "und", index, false, block.first, index - block.first))
        }
    }
}.toTypedArray()

private fun OCRBox.toLegacyBox(): BoundingBoxParcel {
    val dx = (points[1].x.roundToInt() - points[0].x.roundToInt()).toDouble()
    val dy = (points[1].y.roundToInt() - points[0].y.roundToInt()).toDouble()
    val angle = Math.toDegrees(atan2(dy, dx)).toFloat()
    val radians = Math.toRadians(angle.toDouble())
    val cosine = when (angle) {
        0f -> 1.0
        90f, -90f -> 0.0
        180f, -180f -> -1.0
        else -> cos(radians)
    }
    val sine = when (angle) {
        0f, 180f, -180f -> 0.0
        90f -> 1.0
        -90f -> -1.0
        else -> sin(radians)
    }
    val minX = points.minOf { it.x * cosine + it.y * sine }
    val maxX = points.maxOf { it.x * cosine + it.y * sine }
    val minY = points.minOf { -it.x * sine + it.y * cosine }
    val maxY = points.maxOf { -it.x * sine + it.y * cosine }
    val axisAligned = angle % 90f == 0f
    val originMargin = if (axisAligned) 0.0 else 2.0
    val edgeMargin = if (axisAligned) 0.0 else 1.0
    val left = floor(minX) - originMargin
    val top = floor(minY) - originMargin
    val origin = Point((left * cosine - top * sine).roundToInt(), (left * sine + top * cosine).roundToInt())
    val originX = origin.x * cosine + origin.y * sine
    val originY = -origin.x * sine + origin.y * cosine
    val width = ceil(maxX - originX + edgeMargin).toInt()
    val height = ceil(maxY - originY + edgeMargin).toInt()
    return BoundingBoxParcel(origin.x, origin.y, width, height, angle)
}
