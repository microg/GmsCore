/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.vision.barcode

import android.content.Context
import androidx.annotation.Keep
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.vision.barcode.internal.client.BarcodeDetectorOptions
import com.google.android.gms.vision.barcode.internal.client.INativeBarcodeDetector
import com.google.android.gms.vision.barcode.internal.client.INativeBarcodeDetectorCreator
import com.google.android.gms.dynamic.unwrap
import org.microg.gms.vision.barcode.BarcodeDetector

@Keep
class ChimeraNativeBarcodeDetectorCreator : INativeBarcodeDetectorCreator.Stub() {
    override fun create(context: IObjectWrapper, options: BarcodeDetectorOptions): INativeBarcodeDetector {
        return BarcodeDetector(context.unwrap<Context>()!!, options)
    }
}
