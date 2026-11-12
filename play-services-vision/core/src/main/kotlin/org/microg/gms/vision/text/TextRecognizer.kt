/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text

import android.graphics.Bitmap
import android.graphics.ImageFormat
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.unwrap
import com.google.android.gms.vision.internal.FrameMetadataParcel
import com.google.android.gms.vision.text.internal.client.INativeTextRecognizer
import com.google.android.gms.vision.text.internal.client.LineBoxParcel
import com.google.android.gms.vision.text.internal.client.RecognitionOptions
import java.nio.ByteBuffer

class TextRecognizer(private val helper: TextRecognizerHelper) : INativeTextRecognizer.Stub() {
    private var closed = false

    override fun recognize(image: IObjectWrapper?, metadata: FrameMetadataParcel?): Array<LineBoxParcel> {
        return recognizeWithOptions(image, metadata, null)
    }

    @Synchronized
    override fun recognizeWithOptions(
        image: IObjectWrapper?,
        metadata: FrameMetadataParcel?,
        options: RecognitionOptions?,
    ): Array<LineBoxParcel> {
        if (closed) return emptyArray()
        require(image != null && metadata != null) { "Missing OCR image or metadata" }
        val source = image.unwrap<Any>() ?: throw IllegalArgumentException("Expected OCR Bitmap or NV21 buffer")
        val format = when (source) {
            is Bitmap -> -1
            is ByteBuffer -> ImageFormat.NV21
            else -> throw IllegalArgumentException("Expected OCR Bitmap or NV21 buffer")
        }
        try {
            helper.init()
        } catch (e: IllegalStateException) {
            closed = true
            throw UnsupportedOperationException("Offline OCR is unavailable", e)
        }
        return helper.recognize(source, format, metadata.width, metadata.height, metadata.rotation, options?.region).toLegacyText()
    }

    @Synchronized
    override fun close() {
        if (closed) return
        try {
            helper.close()
        } finally {
            closed = true
        }
    }
}
