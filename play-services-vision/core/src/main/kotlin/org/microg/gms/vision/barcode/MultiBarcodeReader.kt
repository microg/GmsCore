/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.barcode

import com.google.zxing.*
import com.google.zxing.multi.MultipleBarcodeReader
import kotlin.math.max
import kotlin.math.min

class MultiBarcodeReader(val hints: Map<DecodeHintType, *>) : MultipleBarcodeReader, Reader {
    val delegate = MultiFormatReader().apply { setHints(hints) }

    fun multiDecode(image: BinaryBitmap): List<Result> {
        return doDecodeMultiple(image)
    }

    override fun decodeMultiple(image: BinaryBitmap): Array<Result> {
        return multiDecode(image).toTypedArray()
    }

    override fun decodeMultiple(image: BinaryBitmap, hints: MutableMap<DecodeHintType, *>?): Array<Result> {
        return multiDecode(image).toTypedArray()
    }

    override fun decode(image: BinaryBitmap): Result {
        return delegate.decodeWithState(image)
    }

    override fun decode(image: BinaryBitmap, hints: MutableMap<DecodeHintType, *>?): Result {
        return delegate.decodeWithState(image)
    }

    override fun reset() {
        delegate.reset()
    }

    // Derived from com.google.zxing.multi GenericMultipleBarcodeReader
    // Copyright 2009 ZXing authors
    // Licensed under the Apache License, Version 2.0
    private fun doDecodeMultiple(
        image: BinaryBitmap,
        results: MutableList<Result> = arrayListOf(),
        xOffset: Int = 0,
        yOffset: Int = 0,
        currentDepth: Int = 0,
        maxDepth: Int = 2
    ): List<Result> {
        val result = kotlin.runCatching { delegate.decodeWithState(image) }.getOrNull() ?: return results

        if (results.none { it.text == result.text }) {
            results.add(translateResultPoints(result, xOffset, yOffset))
        }

        val resultPoints = result.resultPoints
        if (resultPoints != null && resultPoints.isNotEmpty() && currentDepth + 1 < maxDepth) {
            val width = image.width
            val height = image.height
            var minX = width.toFloat()
            var minY = height.toFloat()
            var maxX = 0.0f
            var maxY = 0.0f

            for (point in resultPoints) {
                if (point != null) {
                    minX = min(point.x, minX)
                    minY = min(point.y, minY)
                    maxX = max(point.x, maxX)
                    maxY = max(point.y, maxY)
                }
            }

            if (minX > 100.0f) {
                this.doDecodeMultiple(image.crop(0, 0, minX.toInt(), height), results, xOffset, yOffset, currentDepth + 1, maxDepth)
            }

            if (minY > 100.0f) {
                this.doDecodeMultiple(image.crop(0, 0, width, minY.toInt()), results, xOffset, yOffset, currentDepth + 1, maxDepth)
            }

            if (maxX < (width - 100).toFloat()) {
                this.doDecodeMultiple(image.crop(maxX.toInt(), 0, width - maxX.toInt(), height), results, xOffset + maxX.toInt(), yOffset, currentDepth + 1, maxDepth)
            }

            if (maxY < (height - 100).toFloat()) {
                this.doDecodeMultiple(image.crop(0, maxY.toInt(), width, height - maxY.toInt()), results, xOffset, yOffset + maxY.toInt(), currentDepth + 1, maxDepth)
            }
        }
        return results
    }

    private fun translateResultPoints(result: Result, xOffset: Int, yOffset: Int): Result {
        val oldResultPoints = result.resultPoints
        if (oldResultPoints == null) {
            return result
        } else {
            val newResultPoints = arrayOfNulls<ResultPoint>(oldResultPoints.size)

            for (i in oldResultPoints.indices) {
                val oldPoint = oldResultPoints[i]
                if (oldPoint != null) {
                    newResultPoints[i] = ResultPoint(oldPoint.x + xOffset.toFloat(), oldPoint.y + yOffset.toFloat())
                }
            }

            val newResult = Result(result.text, result.rawBytes, result.numBits, newResultPoints, result.barcodeFormat, result.timestamp)
            newResult.putAllMetadata(result.resultMetadata)
            return newResult
        }
    }

}