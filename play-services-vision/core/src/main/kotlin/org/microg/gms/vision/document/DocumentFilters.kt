/**
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.document

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.File
import kotlin.math.max

private const val BACKGROUND_SCALE = 0.25
private const val CONTRAST_CLIP_LIMIT = 1.5
private const val THRESHOLD_OFFSET = 15.0

enum class DocumentFilter { ORIGINAL, AUTO, GRAYSCALE, BLACK_AND_WHITE }

fun applyFilter(source: File, filter: DocumentFilter, output: File, removeShadows: Boolean) {
    if (filter == DocumentFilter.ORIGINAL) {
        source.copyTo(output, overwrite = true)
        return
    }
    if (!openCvLoaded) throw IllegalStateException("Unable to load OpenCV")
    val bitmap = BitmapFactory.decodeFile(source.absolutePath) ?: throw IllegalArgumentException("Failed to decode $source")
    val rgba = Mat()
    Utils.bitmapToMat(bitmap, rgba)
    bitmap.recycle()
    val rgb = Mat()
    Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB)
    val base = if (removeShadows || filter == DocumentFilter.BLACK_AND_WHITE) removeShadows(rgb) else rgb
    when (filter) {
        DocumentFilter.AUTO -> {
            val enhanced = enhanceContrast(base)
            Imgproc.cvtColor(enhanced, rgba, Imgproc.COLOR_RGB2RGBA)
            enhanced.release()
        }
        DocumentFilter.GRAYSCALE -> {
            val gray = Mat()
            Imgproc.cvtColor(rgb, gray, Imgproc.COLOR_RGB2GRAY)
            Imgproc.cvtColor(gray, rgba, Imgproc.COLOR_GRAY2RGBA)
            gray.release()
        }
        else -> {
            val gray = Mat()
            Imgproc.cvtColor(base, gray, Imgproc.COLOR_RGB2GRAY)
            val blockSize = max(15, max(gray.rows(), gray.cols()) / 40) or 1
            Imgproc.adaptiveThreshold(gray, gray, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, blockSize, THRESHOLD_OFFSET)
            Imgproc.cvtColor(gray, rgba, Imgproc.COLOR_GRAY2RGBA)
            gray.release()
        }
    }
    if (base !== rgb) base.release()
    rgb.release()
    val result = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(rgba, result)
    rgba.release()
    writePage(result, 0f, output)
}

private fun removeShadows(rgb: Mat): Mat {
    val channels = ArrayList<Mat>()
    Core.split(rgb, channels)
    val small = Mat()
    val background = Mat()
    for (channel in channels) {
        Imgproc.resize(channel, small, Size(), BACKGROUND_SCALE, BACKGROUND_SCALE, Imgproc.INTER_AREA)
        val kernelSize = max(3, max(small.rows(), small.cols()) / 100) or 1
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(kernelSize.toDouble(), kernelSize.toDouble()))
        Imgproc.dilate(small, small, kernel)
        kernel.release()
        Imgproc.medianBlur(small, small, kernelSize * 3)
        Imgproc.resize(small, background, channel.size(), 0.0, 0.0, Imgproc.INTER_LINEAR)
        Core.absdiff(channel, background, channel)
        Core.bitwise_not(channel, channel)
        Core.normalize(channel, channel, 0.0, 255.0, Core.NORM_MINMAX)
    }
    small.release()
    background.release()
    val result = Mat()
    Core.merge(channels, result)
    channels.forEach { it.release() }
    return result
}

private fun enhanceContrast(rgb: Mat): Mat {
    val lab = Mat()
    Imgproc.cvtColor(rgb, lab, Imgproc.COLOR_RGB2Lab)
    val channels = ArrayList<Mat>()
    Core.split(lab, channels)
    Imgproc.createCLAHE(CONTRAST_CLIP_LIMIT, Size(8.0, 8.0)).apply(channels[0], channels[0])
    Core.merge(channels, lab)
    channels.forEach { it.release() }
    val result = Mat()
    Imgproc.cvtColor(lab, result, Imgproc.COLOR_Lab2RGB)
    lab.release()
    return result
}
