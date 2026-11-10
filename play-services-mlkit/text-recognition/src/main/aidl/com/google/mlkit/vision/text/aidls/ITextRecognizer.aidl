/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.text.aidls;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.mlkit.vision.text.aidls.FrameMetadataParcel;
import com.google.mlkit.vision.text.aidls.TextResultParcel;

interface ITextRecognizer {
    void init() = 0;
    void close() = 1;
    TextResultParcel process(IObjectWrapper image, in FrameMetadataParcel metadata) = 2;
}
