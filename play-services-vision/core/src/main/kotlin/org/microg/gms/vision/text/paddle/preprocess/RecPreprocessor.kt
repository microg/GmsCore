/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text.paddle.preprocess

import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.ceil

data class RecPreprocessResult(val tensorData: FloatArray, val shape: LongArray)

object RecPreprocessor {
    private const val FIXED_HEIGHT = 48
    private const val MIN_IMG_W = 320
    private const val MAX_IMG_W = 3200
    private const val LIGHT_PADDING_MIN = 240
    private const val MAX_PADDING_SCAN_ROWS = 12
    private const val PADDING_TRIM_ROWS = 2

    internal fun preprocessLatin(crop: Mat): RecPreprocessResult {
        val height = crop.rows()
        if (height < FIXED_HEIGHT + PADDING_TRIM_ROWS) return preprocess(crop)

        val row = ByteArray(crop.cols() * crop.channels())
        fun countLightPadding(firstRow: Int, step: Int): Int {
            for (offset in 0 until MAX_PADDING_SCAN_ROWS) {
                if (crop.get(firstRow + offset * step, 0, row) != row.size ||
                    row.any { (it.toInt() and 0xff) < LIGHT_PADDING_MIN }
                ) return offset
            }
            return MAX_PADDING_SCAN_ROWS
        }

        val top = countLightPadding(0, 1)
        if (top < PADDING_TRIM_ROWS) return preprocess(crop)
        val bottom = countLightPadding(height - 1, -1)
        if (bottom < PADDING_TRIM_ROWS) return preprocess(crop)

        // Move the text toward the center without changing the crop used for geometry.
        val inset = when {
            top - bottom >= 2 * PADDING_TRIM_ROWS -> crop.rowRange(PADDING_TRIM_ROWS, height)
            bottom - top >= 2 * PADDING_TRIM_ROWS -> crop.rowRange(0, height - PADDING_TRIM_ROWS)
            else -> return preprocess(crop)
        }
        return try {
            preprocess(inset)
        } finally {
            inset.release()
        }
    }

    fun preprocess(crop: Mat): RecPreprocessResult {
        require(!crop.empty() && crop.channels() == 3) { "OCR recognition crop must be a nonempty BGR image" }
        val aspectRatio = crop.cols().toDouble() / crop.rows()
        val resizedWidth = ceil(FIXED_HEIGHT * aspectRatio).toInt().coerceIn(1, MAX_IMG_W)
        val width = maxOf(MIN_IMG_W, resizedWidth)
        val resized = Mat()
        val floatMat = Mat()
        val channels = mutableListOf<Mat>()
        return try {
            // Preserve the model's BGR order and normalize each channel to [-1, 1].
            Imgproc.resize(crop, resized, Size(resizedWidth.toDouble(), FIXED_HEIGHT.toDouble()), 0.0, 0.0, Imgproc.INTER_LINEAR)
            resized.convertTo(floatMat, CvType.CV_32F)
            resized.release()
            Core.divide(floatMat, Scalar(127.5, 127.5, 127.5), floatMat)
            Core.subtract(floatMat, Scalar(1.0, 1.0, 1.0), floatMat)
            Core.split(floatMat, channels)
            floatMat.release()
            val channelSize = FIXED_HEIGHT * width
            val tensorData = FloatArray(3 * channelSize)
            val values = FloatArray(FIXED_HEIGHT * resizedWidth)
            for (c in 0..2) {
                channels[c].get(0, 0, values)
                if (width == resizedWidth) {
                    System.arraycopy(values, 0, tensorData, c * channelSize, channelSize)
                } else {
                    // Pad the right side with normalized zero, as in the model's preprocessing.
                    for (row in 0 until FIXED_HEIGHT) {
                        System.arraycopy(values, row * resizedWidth, tensorData, c * channelSize + row * width, resizedWidth)
                    }
                }
            }
            RecPreprocessResult(tensorData, longArrayOf(1, 3, FIXED_HEIGHT.toLong(), width.toLong()))
        } finally {
            channels.forEach { it.release() }
            floatMat.release()
            resized.release()
        }
    }
}
