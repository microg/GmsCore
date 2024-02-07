/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.firstparty.dataservice;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class DeviceManagementInfoResponse extends AbstractSafeParcelable {
    @Field(1)
    public int code;
    @Field(2)
    public String info;
    @Field(3)
    public boolean status;

    public DeviceManagementInfoResponse() {
    }

    public DeviceManagementInfoResponse(int code, String info, boolean status) {
        this.code = code;
        this.info = info;
        this.status = status;
    }

    public DeviceManagementInfoResponse(String info, boolean status) {
        this(1, info, status);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<DeviceManagementInfoResponse> CREATOR = findCreator(DeviceManagementInfoResponse.class);

}
