/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.tflite.dynamite.internal;

import android.os.Parcel;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class CustomerInfo extends AbstractSafeParcelable {
    @Field(1)
    public final String packageName;
    @Field(2)
    public final String versionName;
    @Field(3)
    public final Integer customerId;

    @Constructor
    public CustomerInfo(@Param(1) String packageName, @Param(2) String versionName,
                        @Param(3) Integer customerId) {
        this.packageName = packageName;
        this.versionName = versionName;
        this.customerId = customerId;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<CustomerInfo> CREATOR = findCreator(CustomerInfo.class);
}
