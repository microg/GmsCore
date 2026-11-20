/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.dynamite.descriptors.com.google.android.gms.mlkit_ocr_chinese;

import android.content.Context;

import androidx.annotation.Keep;

import org.microg.gms.vision.text.paddle.model.OcrModelAccess;
import org.microg.gms.vision.text.paddle.model.RecognitionModel;

@Keep
public class ModuleDescriptor {
    public static final String MODULE_ID = "com.google.android.gms.mlkit_ocr_chinese";
    public static final int MODULE_VERSION = com.google.android.gms.dynamite.descriptors.com.google.android.gms.vision.ocr.ModuleDescriptor.MODULE_VERSION;

    public static boolean isAvailable(Context context) {
        return MODULE_VERSION > 0 && OcrModelAccess.isAvailable(context, RecognitionModel.CJK.getPack());
    }
}
