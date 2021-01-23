/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.vision.barcode.internal.client;

import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.internal.client.BarcodeDetectorOptions;
import com.google.android.gms.vision.internal.FrameMetadataParcel;
import com.google.android.gms.dynamic.IObjectWrapper;

interface INativeBarcodeDetector {
    Barcode[] detectBytes(IObjectWrapper byteBuffer, in FrameMetadataParcel metadata) = 0;
    Barcode[] detectBitmap(IObjectWrapper bitmap, in FrameMetadataParcel metadata) = 1;
    void close() = 2;
}
