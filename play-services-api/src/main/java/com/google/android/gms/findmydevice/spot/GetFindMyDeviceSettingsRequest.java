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

@SafeParcelable.Class
public class GetFindMyDeviceSettingsRequest extends AbstractSafeParcelable {
    public static final SafeParcelableCreatorAndWriter<GetFindMyDeviceSettingsRequest> CREATOR = findCreator(GetFindMyDeviceSettingsRequest.class);

    @Field(1)
    public boolean forceRefresh;

    @Constructor
    public GetFindMyDeviceSettingsRequest() {

    }

    @Constructor
    public GetFindMyDeviceSettingsRequest(@Param(1) boolean forceRefresh) {
        this.forceRefresh = forceRefresh;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("GetFindMyDeviceSettingsRequest")
                .field("forceRefresh", forceRefresh)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}