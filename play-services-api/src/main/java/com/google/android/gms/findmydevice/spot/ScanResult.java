/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.findmydevice.spot;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;

import java.util.Arrays;

@SafeParcelable.Class
public class ScanResult extends AbstractSafeParcelable {
    public static final SafeParcelableCreatorAndWriter<ScanResult> CREATOR = findCreator(ScanResult.class);
    
    @Field(1)
    public byte[] address;
    
    @Field(2)
    public int rssi;
    
    @Field(3)
    public String name;
    
    @Field(4)
    public long timestamp;

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("ScanResult")
                .field("address", Arrays.toString(address))
                .field("rssi", rssi)
                .field("name", name)
                .field("timestamp", timestamp)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}