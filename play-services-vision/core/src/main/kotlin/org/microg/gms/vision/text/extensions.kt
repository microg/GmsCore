/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text

import android.graphics.Point
import android.graphics.Rect
import com.google.android.gms.vision.text.internal.client.BoundingBoxParcel
import com.google.android.gms.vision.text.internal.client.LineBoxParcel
import com.google.android.gms.vision.text.internal.client.WordBoxParcel
import com.google.mlkit.vision.text.aidls.TextBlockParcel
import com.google.mlkit.vision.text.aidls.TextElementParcel
import com.google.mlkit.vision.text.aidls.TextLineParcel
import com.google.mlkit.vision.text.aidls.TextResultParcel
import org.microg.gms.vision.text.paddle.model.OCRResult
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

internal fun List<OCRResult>.toMlKitText(): TextResultParcel {
    val blocks = map { result ->
        val points = result.box.points
        val corners = points.map { Point(it.x.roundToInt(), it.y.roundToInt()) }
        val bounds = Rect(
            floor(points.minOf { it.x }).toInt(),
            floor(points.minOf { it.y }).toInt(),
            ceil(points.maxOf { it.x }).toInt(),
            ceil(points.maxOf { it.y }).toInt(),
        )
        val angle = Math.toDegrees(atan2((points[1].y - points[0].y).toDouble(), (points[1].x - points[0].x).toDouble())).toFloat()
        // The model exposes line boxes, so each line is one element without invented word boxes.
        val element = TextElementParcel().apply {
            text = result.text
            boundingBox = bounds
            cornerPoints = corners
            confidence = result.confidence
            this.angle = angle
        }
        val line = TextLineParcel().apply {
            text = result.text
            boundingBox = bounds
            cornerPoints = corners
            elements = listOf(element)
            confidence = result.confidence
            this.angle = angle
        }
        TextBlockParcel().apply {
            text = result.text
            boundingBox = bounds
            cornerPoints = corners
            lines = listOf(line)
        }
    }
    return TextResultParcel().apply {
        text = blocks.joinToString("\n") { it.text }
        this.blocks = blocks
    }
}

internal fun List<OCRResult>.toLegacyText(): Array<LineBoxParcel> = mapIndexed { index, result ->
    val points = result.box.points
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
    val box = BoundingBoxParcel(origin.x, origin.y, width, height, angle)
    val word = WordBoxParcel(emptyArray(), box, box, result.text, result.confidence, "und", false)
    LineBoxParcel(arrayOf(word), box, box, box, result.text, result.confidence, "und", index, false, index, 0)
}.toTypedArray()
