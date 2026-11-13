/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.text.aidls;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.mlkit.vision.common.aidls.ImageMetadataParcel;
import com.google.mlkit.vision.text.aidls.TextParcel;

interface ITextRecognizer {
    void init() = 0;
    void close() = 1;
    TextParcel process(IObjectWrapper image, in ImageMetadataParcel metadata) = 2;
}
