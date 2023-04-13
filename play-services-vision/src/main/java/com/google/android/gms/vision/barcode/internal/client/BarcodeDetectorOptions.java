/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.vision.barcode.internal.client;

import org.microg.safeparcel.AutoSafeParcelable;

public class BarcodeDetectorOptions extends AutoSafeParcelable {
    @Field(1)
    public int versionCode = 1;
    @Field(2)
    public int formats;

    public static Creator<BarcodeDetectorOptions> CREATOR = new AutoCreator<>(BarcodeDetectorOptions.class);
}
