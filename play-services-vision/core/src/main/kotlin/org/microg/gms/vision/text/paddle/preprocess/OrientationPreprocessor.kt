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

data class OrientationPreprocessResult(val tensorData: FloatArray, val shape: LongArray)

object OrientationPreprocessor {
    private const val WIDTH = 160
    private const val HEIGHT = 80
    private val mean = doubleArrayOf(0.485, 0.456, 0.406)
    private val std = doubleArrayOf(0.229, 0.224, 0.225)

    fun preprocess(crop: Mat): OrientationPreprocessResult {
        require(!crop.empty() && crop.type() == CvType.CV_8UC3) { "OCR orientation crop must be a nonempty BGR image" }
        val resized = Mat()
        val floatMat = Mat()
        val channels = mutableListOf<Mat>()
        return try {
            Imgproc.resize(crop, resized, Size(WIDTH.toDouble(), HEIGHT.toDouble()), 0.0, 0.0, Imgproc.INTER_LINEAR)
            resized.convertTo(floatMat, CvType.CV_32F)
            resized.release()
            Core.split(floatMat, channels)
            floatMat.release()
            val channelSize = HEIGHT * WIDTH
            val tensorData = FloatArray(3 * channelSize)
            val values = FloatArray(channelSize)
            for (c in 0..2) {
                // The classifier consumes RGB, while OCR crops use BGR.
                val channel = channels[2 - c]
                Core.multiply(channel, Scalar(1.0 / 255.0), channel)
                Core.subtract(channel, Scalar(mean[c]), channel)
                Core.divide(channel, Scalar(std[c]), channel)
                channel.get(0, 0, values)
                System.arraycopy(values, 0, tensorData, c * channelSize, channelSize)
            }
            OrientationPreprocessResult(tensorData, longArrayOf(1, 3, HEIGHT.toLong(), WIDTH.toLong()))
        } finally {
            channels.forEach { it.release() }
            floatMat.release()
            resized.release()
        }
    }
}
