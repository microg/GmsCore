/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.dynamite.descriptors.com.google.android.gms.vision.ocr;

import android.content.Context;
import androidx.annotation.Keep;
import org.microg.gms.vision.text.paddle.model.OcrModelAccess;
import org.microg.gms.vision.text.paddle.model.RecognitionModel;

@Keep
public final class ModuleDescriptor {
    public static final String MODULE_ID = "com.google.android.gms.vision.ocr";
    public static final int MODULE_VERSION = android.os.Build.VERSION.SDK_INT >= 26 ? 1 : 0;

    public static boolean isAvailable(Context context) {
        return MODULE_VERSION > 0 && OcrModelAccess.isAvailable(context, RecognitionModel.LATIN.getPack());
    }

    private ModuleDescriptor() { }
}
