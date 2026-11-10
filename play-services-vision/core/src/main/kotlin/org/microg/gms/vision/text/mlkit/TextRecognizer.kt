/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text.mlkit

import android.util.Log
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.unwrap
import com.google.mlkit.vision.text.aidls.FrameMetadataParcel
import com.google.mlkit.vision.text.aidls.ITextRecognizer
import com.google.mlkit.vision.text.aidls.TextResultParcel
import org.microg.gms.vision.text.TAG
import org.microg.gms.vision.text.TextRecognizerHelper
import org.microg.gms.vision.text.toMlKitText

class TextRecognizer(private val helper: TextRecognizerHelper) : ITextRecognizer.Stub() {
    override fun init() {
        helper.init()
    }

    override fun process(image: IObjectWrapper?, metadata: FrameMetadataParcel?): TextResultParcel {
        require(image != null && metadata != null) { "Missing OCR image or metadata" }
        val source = try {
            image.unwrap<Any>() ?: throw IllegalArgumentException("OCR image is null")
        } catch (e: Exception) {
            Log.e(TAG, "process: unable to unwrap OCR image", e)
            throw IllegalStateException("Offline text recognition failed", e)
        } catch (e: LinkageError) {
            Log.e(TAG, "process: native OCR runtime failed", e)
            throw IllegalStateException("Native OCR runtime failed", e)
        }
        return helper.recognize(source, metadata.format, metadata.width, metadata.height, metadata.rotation).toMlKitText()
    }

    override fun close() {
        helper.close()
    }
}
