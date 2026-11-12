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
import java.nio.FloatBuffer
import kotlin.math.round

data class DetPreprocessResult(val tensorData: FloatBuffer, val shape: LongArray)

object DetPreprocessor {
    private val mean = doubleArrayOf(0.485, 0.456, 0.406)
    private val std = doubleArrayOf(0.229, 0.224, 0.225)

    fun preprocess(src: Mat, inputBuffer: (Int) -> FloatBuffer): DetPreprocessResult {
        // TextInputImage already limits the image size; the detector needs multiples of 32.
        val h = maxOf(round(src.rows() / 32.0).toInt() * 32, 32)
        val w = maxOf(round(src.cols() / 32.0).toInt() * 32, 32)
        val resized = Mat()
        val floatMat = Mat()
        val channels = mutableListOf<Mat>()
        return try {
            val input = if (src.rows() == h && src.cols() == w) {
                src
            } else {
                Imgproc.resize(src, resized, Size(w.toDouble(), h.toDouble()), 0.0, 0.0, Imgproc.INTER_LINEAR)
                resized
            }
            input.convertTo(floatMat, CvType.CV_32F)
            resized.release()
            Core.split(floatMat, channels)
            floatMat.release()
            val channelSize = h * w
            val tensorData = inputBuffer(3 * channelSize)
            val values = FloatArray(channelSize)
            for (c in 0..2) {
                Core.multiply(channels[c], Scalar(1.0 / 255.0), channels[c])
                Core.subtract(channels[c], Scalar(mean[c]), channels[c])
                Core.divide(channels[c], Scalar(std[c]), channels[c])
                channels[c].get(0, 0, values)
                tensorData.put(values)
            }
            tensorData.flip()
            DetPreprocessResult(tensorData, longArrayOf(1, 3, h.toLong(), w.toLong()))
        } finally {
            channels.forEach { it.release() }
            floatMat.release()
            resized.release()
        }
    }
}
