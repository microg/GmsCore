/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.barcode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.Image
import android.os.Build.VERSION.SDK_INT
import android.os.Parcel
import android.util.Log
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import com.google.android.gms.dynamic.unwrap
import com.google.mlkit.vision.barcode.aidls.IBarcodeScanner
import com.google.mlkit.vision.barcode.internal.*
import com.google.zxing.BarcodeFormat
import org.microg.gms.utils.warnOnTransactionIssues
import java.nio.ByteBuffer

private const val TAG = "BarcodeScanner"

class BarcodeScanner(val context: Context, val options: BarcodeScannerOptions) : IBarcodeScanner.Stub() {
    private val helper =
        BarcodeDecodeHelper(if (options.allPotentialBarcodesEnabled) BarcodeFormat.values().toList() else options.supportedFormats.mlKitToZXingBarcodeFormats())
    private var loggedOnce = false

    override fun init() {
        Log.d(TAG, "init()")
    }

    override fun close() {
        Log.d(TAG, "close()")
    }

    override fun detect(wrappedImage: IObjectWrapper, metadata: ImageMetadata): List<Barcode> {
        if (!loggedOnce) Log.d(TAG, "detect(${ObjectWrapper.unwrap(wrappedImage)}, $metadata)").also { loggedOnce = true }
        return when (metadata.format) {
            -1 -> wrappedImage.unwrap<Bitmap>()?.let { helper.decodeFromBitmap(it) }
            ImageFormat.NV21 -> wrappedImage.unwrap<ByteBuffer>()?.let { helper.decodeFromLuminanceBytes(it, metadata.width, metadata.height, metadata.rotation) }
            ImageFormat.YUV_420_888 -> if (SDK_INT >= 19) wrappedImage.unwrap<Image>()?.let { image -> helper.decodeFromImage(image, metadata.rotation) } else null

            else -> null
        }?.map { it.toMlKit(metadata) } ?: emptyList()
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}