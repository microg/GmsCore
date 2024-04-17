/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.barcode.internal;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class WiFi extends AbstractSafeParcelable {
    @Field(1)
    public String ssid;
    @Field(2)
    public String password;
    @Field(3)
    public int encryptionType;

    // TODO: Copied from com.google.mlkit.vision.barcode.common.Barcode.WiFi
    public static final int OPEN = 1;
    public static final int WPA = 2;
    public static final int WEP = 3;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<WiFi> CREATOR = findCreator(WiFi.class);
}
