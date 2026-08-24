/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.findmydevice.spot;

import android.accounts.Account;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class GetCachedDevicesRequest extends AbstractSafeParcelable {
    public static final SafeParcelableCreatorAndWriter<GetCachedDevicesRequest> CREATOR = findCreator(GetCachedDevicesRequest.class);

    @Field(1)
    public Account account;

    @Constructor
    public GetCachedDevicesRequest() {

    }

    @Constructor
    public GetCachedDevicesRequest(@Param(1) Account account) {
        this.account = account;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("GetCachedDevicesRequest")
                .field("account", account)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}