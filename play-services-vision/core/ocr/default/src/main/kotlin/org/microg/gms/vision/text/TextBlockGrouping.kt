/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text

import org.microg.gms.vision.text.paddle.model.OCRBox
import org.microg.gms.vision.text.paddle.model.OCRResult
import kotlin.math.abs

internal fun List<OCRResult>.groupTextLines(): List<IntRange> {
    if (isEmpty()) return emptyList()
    val blocks = mutableListOf<IntRange>()
    var start = 0
    var first = this[0].box.horizontalBounds()
    var previous = first
    for (index in 1..lastIndex) {
        val current = this[index].box.horizontalBounds()
        if (current == null || previous == null || first == null ||
            !previous.isAlignedWith(current) || !first.isAlignedWith(current) ||
            current.top - previous.bottom !in 0f..minOf(current.height, previous.height) * 0.75f
        ) {
            blocks.add(start until index)
            start = index
            first = current
        }
        previous = current
    }
    blocks.add(start..lastIndex)
    return blocks
}

private fun OCRBox.horizontalBounds(): LineBounds? {
    val (topLeft, topRight, bottomRight, bottomLeft) = points
    val topWidth = topRight.x - topLeft.x
    val bottomWidth = bottomRight.x - bottomLeft.x
    if (topWidth <= 0f || bottomWidth <= 0f ||
        abs(topRight.y - topLeft.y) > topWidth * 0.08f ||
        abs(bottomRight.y - bottomLeft.y) > bottomWidth * 0.08f
    ) return null

    val bounds = LineBounds(
        minOf(topLeft.x, bottomLeft.x), minOf(topLeft.y, topRight.y),
        maxOf(topRight.x, bottomRight.x), maxOf(bottomLeft.y, bottomRight.y),
    )
    return bounds.takeIf { it.height > 0f && it.width >= 2 * it.height }
}

private data class LineBounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    fun isAlignedWith(other: LineBounds): Boolean {
        val minHeight = minOf(height, other.height)
        val minWidth = minOf(width, other.width)
        val horizontalOverlap = minOf(right, other.right) - maxOf(left, other.left)
        return minHeight >= 0.75f * maxOf(height, other.height) &&
                minWidth >= 0.5f * maxOf(width, other.width) &&
                abs(left - other.left) <= 0.5f * minHeight && horizontalOverlap >= 0.85f * minWidth
    }
}
