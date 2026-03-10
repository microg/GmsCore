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
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.ChecksumException
import com.google.zxing.DecodeHintType
import com.google.zxing.FormatException
import com.google.zxing.LuminanceSource
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer
import java.nio.IntBuffer

private const val TAG = "BarcodeDecodeHelper"

class BarcodeDecodeHelper(formats: List<BarcodeFormat>, multi: Boolean = true) {
    private val reader = MultiBarcodeReader(
        mapOf(
            DecodeHintType.TRY_HARDER to true,
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

    fun decodeFromLuminanceBytes(rawBarcodeData: RawBarcodeData, rotate: Int): List<Result> {
        Log.d(TAG, "decodeFromLuminanceBytes rotate:")
        rawBarcodeData.rotateDetail(rotate)
        return decodeFromSource(
            PlanarYUVLuminanceSource(
                rawBarcodeData.bytes, rawBarcodeData.width, rawBarcodeData.height,
                0, 0, rawBarcodeData.width, rawBarcodeData.height, false
            )
        )
    }

    fun decodeFromLuminanceBytes(buffer: ByteBuffer, width: Int, height: Int, rotate: Int = 0): List<Result> {
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        buffer.rewind()
        val rawBarcodeData = RawBarcodeData(bytes, width, height)
        return decodeFromLuminanceBytes(rawBarcodeData, rotate)
    }

    @RequiresApi(19)
    fun decodeFromImage(image: Image, rotate: Int = 0): List<Result> {
        if (image.format !in SUPPORTED_IMAGE_FORMATS) return emptyList()
        val rawBarcodeData =RawBarcodeData(getYUVBytesFromImage(image), image.width, image.height)
        return decodeFromLuminanceBytes(rawBarcodeData, rotate)
    }

    private fun getYUVBytesFromImage(image: Image): ByteArray {
        val planes = image.planes
        val width = image.width
        val height = image.height
        val yuvBytes = ByteArray(width * height * 3 / 2)
        var offset = 0

        for (i in planes.indices) {
            val buffer = planes[i].buffer
            val rowStride = planes[i].rowStride
            val pixelStride = planes[i].pixelStride
            val planeWidth = if ((i == 0)) width else width / 2
            val planeHeight = if ((i == 0)) height else height / 2

            val planeBytes = ByteArray(buffer.capacity())
            buffer[planeBytes]

            for (row in 0 until planeHeight) {
                for (col in 0 until planeWidth) {
                    yuvBytes[offset++] = planeBytes[row * rowStride + col * pixelStride]
                }
            }
        }
        return yuvBytes
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