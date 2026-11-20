/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text.mlkit

import android.graphics.Point
import android.graphics.Rect
import com.google.mlkit.vision.text.aidls.TextBlockParcel
import com.google.mlkit.vision.text.aidls.TextElementParcel
import com.google.mlkit.vision.text.aidls.TextLineParcel
import com.google.mlkit.vision.text.aidls.TextParcel
import org.microg.gms.vision.text.groupTextLines
import org.microg.gms.vision.text.model.OCRBox
import org.microg.gms.vision.text.model.OCRResult
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

internal fun List<OCRResult>.toMlKitText(): TextParcel {
    val blocks = groupTextLines().map { block ->
        val blockLines = block.map { this[it].toMlKitLine() }
        val first = blockLines.first()
        TextBlockParcel().apply {
            text = blockLines.joinToString("\n") { it.text }
            lines = blockLines
            if (blockLines.size == 1) {
                boundingBox = first.boundingBox
                cornerPoints = first.cornerPoints
            } else {
                val bounds = Rect(first.boundingBox)
                for (index in 1..blockLines.lastIndex) bounds.union(blockLines[index].boundingBox)
                boundingBox = bounds
                cornerPoints = listOf(
                    Point(bounds.left, bounds.top), Point(bounds.right, bounds.top),
                    Point(bounds.right, bounds.bottom), Point(bounds.left, bounds.bottom),
                )
            }
        }
    }
    return TextParcel().apply {
        text = this@toMlKitText.joinToString("\n") { it.text }
        this.blocks = blocks
    }
}

private fun OCRResult.toMlKitLine(): TextLineParcel {
    val geometry = TextGeometry(box, readingStartCorner)
    val lineElements = if (words.isEmpty()) {
        listOf(geometry.element(text, confidence))
    } else {
        words.map { TextGeometry(it.box).element(it.text, confidence) }
    }
    return TextLineParcel().apply {
        text = this@toMlKitLine.text
        boundingBox = geometry.bounds
        cornerPoints = geometry.corners
        elements = lineElements
        confidence = this@toMlKitLine.confidence
        angle = geometry.angle
    }
}

private class TextGeometry(box: OCRBox, readingStartCorner: Int = 0) {
    private val points = box.points
    val corners = points.map { Point(it.x.roundToInt(), it.y.roundToInt()) }
    val bounds = Rect(
        floor(points.minOf { it.x }).toInt(),
        floor(points.minOf { it.y }).toInt(),
        ceil(points.maxOf { it.x }).toInt(),
        ceil(points.maxOf { it.y }).toInt(),
    )
    val angle: Float = run {
        fun corner(offset: Int) = points[(readingStartCorner + offset) % points.size]
        val start = corner(0)
        val end = corner(1)
        var dx = end.x - start.x
        var dy = end.y - start.y
        // The corresponding edge can still provide orientation if clipping collapsed this one.
        if (dx == 0f && dy == 0f) {
            dx = corner(2).x - corner(3).x
            dy = corner(2).y - corner(3).y
        }
        val degrees = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        if (degrees == -180f) 180f else degrees
    }

    fun element(value: String, score: Float) = TextElementParcel().apply {
        text = value
        boundingBox = bounds
        cornerPoints = corners
        confidence = score
        angle = this@TextGeometry.angle
    }
}
