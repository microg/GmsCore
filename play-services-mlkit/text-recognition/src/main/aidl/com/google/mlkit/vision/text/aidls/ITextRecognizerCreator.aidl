/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.text.aidls;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.mlkit.vision.text.aidls.ITextRecognizer;
import com.google.mlkit.vision.text.aidls.TextRecognizerOptionsParcel;

interface ITextRecognizerCreator {
    ITextRecognizer create(IObjectWrapper context) = 0;
    ITextRecognizer createWithOptions(IObjectWrapper context, in TextRecognizerOptionsParcel options) = 1;
}
