package com.google.mlkit.vision.barcode.aidls;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.mlkit.vision.barcode.internal.Barcode;
import com.google.mlkit.vision.barcode.internal.ImageMetadata;

interface IBarcodeScanner {
    void init() = 0;
    void close() = 1;
    List<Barcode> detect(IObjectWrapper image, in ImageMetadata metadata) = 2;
}