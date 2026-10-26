/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.rcs;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class RcsConfigRequest extends AbstractSafeParcelable {
    @Field(1)
    public String packageName;

    @Field(2)
    public int subId;

    @Field(3)
    public String msisdn;

    public RcsConfigRequest() {
    }

    public RcsConfigRequest(String packageName, int subId, String msisdn) {
        this.packageName = packageName;
        this.subId = subId;
        this.msisdn = msisdn;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("RcsConfigRequest")
                .field("packageName", packageName)
                .field("subId", subId)
                .field("msisdn", msisdn)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<RcsConfigRequest> CREATOR = findCreator(RcsConfigRequest.class);
}
