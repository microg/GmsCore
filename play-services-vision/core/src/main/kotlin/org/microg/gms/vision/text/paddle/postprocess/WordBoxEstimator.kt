/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text.paddle.postprocess

import android.graphics.RectF
import org.microg.gms.vision.text.paddle.model.OCRWord
import org.opencv.core.CvException
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.ceil

internal object WordBoxEstimator {
    const val MAX_LINES_PER_IMAGE = 12
    private const val MAX_WIDTH = 768
    private const val MAX_HEIGHT = 40
    private const val MAX_WORDS = 12
    private val separator = Regex("[\\p{Zs}\\t]+")

    fun estimate(crop: QuadTextCrop.Crop, text: String, rotated180: Boolean): List<OCRWord> {
        if (text.length !in 3..256 || '\n' in text || '\r' in text) return emptyList()
        val words = text.trim().split(separator, limit = MAX_WORDS + 1)
        if (words.size !in 2..MAX_WORDS) return emptyList()
        val image = crop.image
        if (image.cols() !in 8..MAX_WIDTH * 2 || image.rows() !in 8..MAX_HEIGHT * 4) return emptyList()
        val width = minOf(image.cols(), MAX_WIDTH)
        val height = minOf(image.rows(), MAX_HEIGHT)
        val scaleX = image.cols().toFloat() / width
        val scaleY = image.rows().toFloat() / height
        return try {
            val gray = grayscale(image, width, height)
            val bounds = findBounds(gray, width, height, scaleX, scaleY, words.size) ?: return emptyList()
            val boxes = crop.toSourceBoxes(bounds, rotated180) ?: return emptyList()
            words.indices.map { OCRWord(boxes[it], words[it]) }
        } catch (_: CvException) {
            emptyList()
        }
    }

    private fun grayscale(image: Mat, width: Int, height: Int): ByteArray {
        val gray = Mat()
        try {
            if (width == image.cols() && height == image.rows()) {
                Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY)
            } else {
                val sample = Mat()
                try {
                    Imgproc.resize(image, sample, Size(width.toDouble(), height.toDouble()), 0.0, 0.0, Imgproc.INTER_AREA)
                    Imgproc.cvtColor(sample, gray, Imgproc.COLOR_BGR2GRAY)
                } finally {
                    sample.release()
                }
            }
            return ByteArray(width * height).also { gray.get(0, 0, it) }
        } finally {
            gray.release()
        }
    }

    private fun findBounds(
        pixels: ByteArray, width: Int, height: Int, scaleX: Float, scaleY: Float, wordCount: Int,
    ): List<RectF>? {
        val histogram = IntArray(256)
        for (pixel in pixels) histogram[pixel.toInt() and 255]++
        fun percentile(rank: Int): Int {
            var count = 0
            for (value in histogram.indices) {
                count += histogram[value]
                if (count >= rank) return value
            }
            return 255
        }

        val tail = pixels.size / 20
        val dark = percentile(tail)
        val light = percentile(pixels.size - tail)
        if (light - dark < 48) return null
        val threshold = (dark + light) / 2
        fun isLight(index: Int) = (pixels[index].toInt() and 255) > threshold

        var lightBorder = 0
        for (x in 0 until width) {
            if (isLight(x)) lightBorder++
            if (isLight((height - 1) * width + x)) lightBorder++
        }
        for (y in 1 until height - 1) {
            if (isLight(y * width)) lightBorder++
            if (isLight(y * width + width - 1)) lightBorder++
        }
        val darkBorder = 2 * (width + height - 2) - lightBorder
        if (minOf(lightBorder, darkBorder) * 3 > maxOf(lightBorder, darkBorder)) return null
        val lightBackground = lightBorder > darkBorder

        val top = IntArray(width) { height }
        val bottom = IntArray(width) { -1 }
        for (y in 0 until height) for (x in 0 until width) {
            if (isLight(y * width + x) == lightBackground) continue
            if (top[x] == height) top[x] = y
            bottom[x] = y
        }
        val first = top.indexOfFirst { it < height }
        val last = bottom.indexOfLast { it >= 0 }
        val inkHeight = bottom.maxOrNull()!! - top.minOrNull()!! + 1
        if (first < 0 || inkHeight < 4) return null
        val minimumGap = maxOf(2, ceil(0.22 * inkHeight * scaleY / scaleX).toInt())

        val spans = ArrayList<IntRange>(wordCount)
        var wordStart = first
        var column = first + 1
        var smallestSeparator = Int.MAX_VALUE
        var largestIgnoredGap = 0
        while (column < last) {
            if (top[column] < height) {
                column++
                continue
            }
            val gapStart = column
            while (column < last && top[column] == height) column++
            val gap = column - gapStart
            if (gap >= minimumGap) {
                spans.add(wordStart until gapStart)
                if (spans.size >= wordCount) return null
                wordStart = column
                smallestSeparator = minOf(smallestSeparator, gap)
            } else {
                largestIgnoredGap = maxOf(largestIgnoredGap, gap)
            }
        }
        spans.add(wordStart..last)
        if (spans.size != wordCount || largestIgnoredGap * 4 >= smallestSeparator * 3) return null
        return spans.map { span ->
            val wordTop = span.minOf { top[it] }
            val wordBottom = span.maxOf { bottom[it] } + 1
            if (span.last == span.first || wordBottom - wordTop < 3) return null
            RectF(span.first * scaleX, wordTop * scaleY, (span.last + 1) * scaleX, wordBottom * scaleY)
        }
    }
}
