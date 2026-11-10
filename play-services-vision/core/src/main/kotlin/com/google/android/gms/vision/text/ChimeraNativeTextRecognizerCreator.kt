/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.vision.text

import android.content.Context
import androidx.annotation.Keep
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.unwrap
import com.google.android.gms.vision.text.internal.client.INativeTextRecognizer
import com.google.android.gms.vision.text.internal.client.INativeTextRecognizerCreator
import com.google.android.gms.vision.text.internal.client.TextRecognizerOptions
import org.microg.gms.common.Constants
import org.microg.gms.vision.text.TextRecognizerHelper
import org.microg.gms.vision.text.TextRecognizer as NativeTextRecognizer

@Keep
open class ChimeraNativeTextRecognizerCreator : INativeTextRecognizerCreator.Stub() {
    override fun create(context: IObjectWrapper, options: TextRecognizerOptions?): INativeTextRecognizer {
        val caller = context.unwrap<Context>() ?: throw IllegalArgumentException("OCR context is null")
        val modelContext = caller.createPackageContext(Constants.GMS_PACKAGE_NAME, 0)
        val helper = TextRecognizerHelper(modelContext)
        helper.init()
        return NativeTextRecognizer(helper)
    }
}
