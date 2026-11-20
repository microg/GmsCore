/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.vision.text.mlkit

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.unwrap
import com.google.mlkit.vision.text.aidls.ITextRecognizer
import com.google.mlkit.vision.text.aidls.ITextRecognizerCreator
import com.google.mlkit.vision.text.aidls.TextRecognizerOptions
import org.microg.gms.vision.text.TAG
import org.microg.gms.vision.text.TextRecognizerHelper
import org.microg.gms.vision.text.mlkit.TextRecognizer
import org.microg.gms.vision.text.model.RecognitionModel

@Keep
class TextRecognizerCreator : ITextRecognizerCreator.Stub() {
    override fun create(context: IObjectWrapper): ITextRecognizer = newRecognizer(context, RecognitionModel.LATIN)

    override fun createWithOptions(context: IObjectWrapper, options: TextRecognizerOptions?): ITextRecognizer {
        require(options == null || options.customModel.isNullOrEmpty()) { "Custom OCR models are not supported" }
        val model = when (options?.languageOption ?: 1) {
            1 -> RecognitionModel.LATIN
            2 -> RecognitionModel.CJK
            4 -> RecognitionModel.JAPANESE
            5 -> RecognitionModel.KOREAN
            else -> throw IllegalArgumentException("Unsupported OCR script option")
        }
        return newRecognizer(context, model)
    }

    private fun newRecognizer(wrapper: IObjectWrapper, model: RecognitionModel): ITextRecognizer {
        check(android.os.Build.VERSION.SDK_INT >= 26) { "Offline OCR requires Android API 26 or newer" }
        val caller = wrapper.unwrap<Context>() ?: throw IllegalArgumentException("OCR context is null")
        Log.d(TAG, "create: offline OCR model=$model")
        return TextRecognizer(TextRecognizerHelper(caller, model))
    }
}
