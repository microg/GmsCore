/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.barcode

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.Image
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer
import java.nio.IntBuffer

private const val TAG = "BarcodeDecodeHelper"

class BarcodeDecodeHelper(formats: List<BarcodeFormat>, multi: Boolean = true) {
    private val reader = MultiBarcodeReader(
        mapOf(
            DecodeHintType.ALSO_INVERTED to true,
            DecodeHintType.POSSIBLE_FORMATS to formats
        )
    )

    fun decodeFromSource(source: LuminanceSource): List<Result> {
        return try {
            reader.multiDecode(BinaryBitmap(HybridBinarizer(source))).also {
                if (it.isNotEmpty()) reader.reset()
            }
        } catch (e: NotFoundException) {
            emptyList()
        } catch (e: FormatException) {
            emptyList()
        } catch (e: ChecksumException) {
            emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Exception with $this: $e")
            emptyList()
        }
    }

    fun decodeFromLuminanceBytes(bytes: ByteArray, width: Int, height: Int, rowStride: Int = width): List<Result> {
        return decodeFromSource(PlanarYUVLuminanceSource(bytes, rowStride, height, 0, 0, width, height, false))
    }

    fun decodeFromLuminanceBytes(buffer: ByteBuffer, width: Int, height: Int, rowStride: Int = width): List<Result> {
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        buffer.rewind()
        return decodeFromLuminanceBytes(bytes, width, height, rowStride)
    }

    @RequiresApi(19)
    fun decodeFromImage(image: Image): List<Result> {
        if (image.format !in SUPPORTED_IMAGE_FORMATS) return emptyList()
        val yPlane = image.planes[0]
        return decodeFromLuminanceBytes(yPlane.buffer, image.width, image.height, yPlane.rowStride)
    }

    fun decodeFromBitmap(bitmap: Bitmap): List<Result> {
        val frameBuf: IntBuffer = IntBuffer.allocate(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(frameBuf)
        return decodeFromSource(RGBLuminanceSource(bitmap.width, bitmap.height, frameBuf.array()))
    }

    companion object {
        @RequiresApi(19)
        val SUPPORTED_IMAGE_FORMATS =
            listOfNotNull(ImageFormat.YUV_420_888, if (SDK_INT >= 23) ImageFormat.YUV_422_888 else null, if (SDK_INT >= 23) ImageFormat.YUV_444_888 else null)
    }
}