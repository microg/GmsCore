/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.vision.barcode.mlkit

import android.content.Context
import androidx.annotation.Keep
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.unwrap
import com.google.mlkit.vision.barcode.aidls.IBarcodeScannerCreator
import com.google.mlkit.vision.barcode.aidls.IBarcodeScanner
import com.google.mlkit.vision.barcode.internal.BarcodeScannerOptions
import org.microg.gms.vision.barcode.BarcodeScanner

@Keep
class BarcodeScannerCreator : IBarcodeScannerCreator.Stub() {
    override fun create(context: IObjectWrapper, options: BarcodeScannerOptions): IBarcodeScanner {
        return BarcodeScanner(context.unwrap<Context>()!!, options)
    }
}