/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.utils

import android.graphics.Bitmap
import kotlin.math.sqrt

object BitmapUtils {

    fun getBitmapSize(bitmap: Bitmap?): Int {
        if (bitmap != null) {
            return bitmap.height * bitmap.rowBytes
        }
        return 0
    }

    fun scaledBitmap(bitmap: Bitmap, maxSize: Float): Bitmap {
        val height: Int = bitmap.getHeight()
        val width: Int = bitmap.getWidth()
        val sqrt =
            sqrt(((maxSize) / ((width.toFloat()) / (height.toFloat()) * ((bitmap.getRowBytes() / width).toFloat()))).toDouble())
                .toInt()
        return Bitmap.createScaledBitmap(
            bitmap,
            (((sqrt.toFloat()) / (height.toFloat()) * (width.toFloat())).toInt()),
            sqrt,
            true
        )
    }
}