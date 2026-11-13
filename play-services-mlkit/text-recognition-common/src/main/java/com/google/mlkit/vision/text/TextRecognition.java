/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.text;

import androidx.annotation.NonNull;
import org.microg.gms.common.PublicApi;
import org.microg.mlkit.vision.text.TextRecognizerImpl;

@PublicApi
public class TextRecognition {
    private TextRecognition() {
    }

    @NonNull
    public static TextRecognizer getClient(@NonNull TextRecognizerOptionsInterface options) {
        if (options == null) throw new NullPointerException("Text recognizer options must not be null");
        return new TextRecognizerImpl(options);
    }
}
