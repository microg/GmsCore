/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.internal;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;

import java.util.List;

@SafeParcelable.Class
public class TelemetryData extends AbstractSafeParcelable {

    @Field(1)
    public int dataType;
    @Field(2)
    public List<MethodInvocation> dataList;

    public TelemetryData() {
    }

    public TelemetryData(int dataType, List<MethodInvocation> dataList) {
        this.dataType = dataType;
        this.dataList = dataList;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("TelemetryData")
                .field("dataType", dataType)
                .field("dataList", dataList)
                .end();
    }

    public static SafeParcelableCreatorAndWriter<TelemetryData> CREATOR = findCreator(TelemetryData.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

}