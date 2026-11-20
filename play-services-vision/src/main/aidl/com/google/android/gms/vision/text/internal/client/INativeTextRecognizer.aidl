/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.vision.text.internal.client;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.vision.internal.FrameMetadataParcel;
import com.google.android.gms.vision.text.internal.client.LineBoxParcel;
import com.google.android.gms.vision.text.internal.client.RecognitionOptions;

interface INativeTextRecognizer {
    LineBoxParcel[] recognize(IObjectWrapper image, in FrameMetadataParcel metadata) = 0;
    void close() = 1;
    LineBoxParcel[] recognizeWithOptions(IObjectWrapper image, in FrameMetadataParcel metadata, in RecognitionOptions options) = 2;
}
