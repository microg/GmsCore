/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.barcode

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.unwrap
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.internal.client.BarcodeDetectorOptions
import com.google.android.gms.vision.barcode.internal.client.INativeBarcodeDetector
import com.google.android.gms.vision.internal.FrameMetadataParcel
import com.google.zxing.*
import com.google.zxing.aztec.AztecReader
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.datamatrix.DataMatrixReader
import com.google.zxing.multi.MultipleBarcodeReader
import com.google.zxing.multi.qrcode.QRCodeMultiReader
import com.google.zxing.oned.*
import com.google.zxing.pdf417.PDF417Reader
import java.nio.ByteBuffer
import java.nio.IntBuffer

private const val TAG = "GmsVisionBarcode"

class BarcodeDetector(val context: Context, val options: BarcodeDetectorOptions) : INativeBarcodeDetector.Stub() {
    override fun detectBitmap(wrappedBitmap: IObjectWrapper, metadata: FrameMetadataParcel): Array<Barcode> {
        val bitmap = wrappedBitmap.unwrap<Bitmap>() ?: return emptyArray()
        val frameBuf: IntBuffer = IntBuffer.allocate(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(frameBuf)
        return detectFromSource(RGBLuminanceSource(metadata.width, metadata.height, frameBuf.array()))
    }

    override fun detectBytes(wrappedByteBuffer: IObjectWrapper, metadata: FrameMetadataParcel): Array<Barcode> {
        return detectFromSource(DirectLuminanceSource(metadata.width, metadata.height, wrappedByteBuffer.unwrap<ByteBuffer>()
                ?: return emptyArray()))
    }

    private fun mayDecodeType(image: BinaryBitmap, type: Int, reader: () -> Reader): List<Barcode> {
        return if ((options.formats and type) != 0 || options.formats == Barcode.ALL_FORMATS) {
            reader().run {
                try {
                    if (this is MultipleBarcodeReader) {
                        decodeMultiple(image).map { it.toGms() }
                    } else {
                        listOf(decode(image).toGms())
                    }
                } catch (e: NotFoundException) {
                    emptyList<Barcode>()
                } catch (e: FormatException) {
                    emptyList<Barcode>()
                } catch (e: ChecksumException) {
                    emptyList<Barcode>()
                } catch (e: Exception) {
                    Log.w(TAG, "Exception with $this: $e")
                    emptyList<Barcode>()
                }
            }
        } else {
            emptyList()
        }
    }

    private fun detectFromImage(image: BinaryBitmap, results: MutableList<Barcode>): Int {
        var resultsSize = results.size
        results.addAll(mayDecodeType(image, Barcode.CODE_128) { Code128Reader() })
        results.addAll(mayDecodeType(image, Barcode.CODE_39) { Code39Reader() })
        results.addAll(mayDecodeType(image, Barcode.CODABAR) { CodaBarReader() })
        results.addAll(mayDecodeType(image, Barcode.DATA_MATRIX) { DataMatrixReader() })
        results.addAll(mayDecodeType(image, Barcode.EAN_13) { EAN13Reader() })
        results.addAll(mayDecodeType(image, Barcode.EAN_8) { EAN8Reader() })
        results.addAll(mayDecodeType(image, Barcode.ITF) { ITFReader() })
        results.addAll(mayDecodeType(image, Barcode.QR_CODE) { QRCodeMultiReader() })
        results.addAll(mayDecodeType(image, Barcode.UPC_A) { UPCAReader() })
        results.addAll(mayDecodeType(image, Barcode.UPC_E) { UPCEReader() })
        results.addAll(mayDecodeType(image, Barcode.PDF417) { PDF417Reader() })
        results.addAll(mayDecodeType(image, Barcode.AZTEC) { AztecReader() })
        return results.size - resultsSize
    }

    private fun detectFromSource(source: LuminanceSource): Array<Barcode> {
        val results = arrayListOf<Barcode>()
        try {
            detectFromImage(BinaryBitmap(HybridBinarizer(source)), results)
            detectFromImage(BinaryBitmap(HybridBinarizer(source.invert())), results)
        } catch (e: Exception) {
            Log.w(TAG, e)
        }

        return results.distinctBy { it.rawValue }.toTypedArray()
    }

    override fun close() {
        Log.d(TAG, "close()")
    }
}
