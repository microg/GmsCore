package com.google.mlkit.vision.barcode.aidls;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.mlkit.vision.barcode.aidls.IBarcodeScanner;
import com.google.mlkit.vision.barcode.internal.BarcodeScannerOptions;

interface IBarcodeScannerCreator {
    IBarcodeScanner create(IObjectWrapper wrappedContext, in BarcodeScannerOptions options) = 0;
}