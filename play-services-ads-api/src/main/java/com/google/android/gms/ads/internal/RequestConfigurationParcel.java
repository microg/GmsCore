/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class RequestConfigurationParcel extends AbstractSafeParcelable {
    @Field(1)
    public final int tagForChildDirectedTreatment;
    @Field(2)
    public final int tagForUnderAgeOfConsent;

    @Constructor
    RequestConfigurationParcel(@Param(1) int tagForChildDirectedTreatment, @Param(2) int tagForUnderAgeOfConsent) {
        this.tagForChildDirectedTreatment = tagForChildDirectedTreatment;
        this.tagForUnderAgeOfConsent = tagForUnderAgeOfConsent;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<RequestConfigurationParcel> CREATOR = findCreator(RequestConfigurationParcel.class);
}
