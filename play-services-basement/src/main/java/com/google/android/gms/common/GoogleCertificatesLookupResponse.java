/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common;

import android.os.Parcel;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class GoogleCertificatesLookupResponse extends AbstractSafeParcelable {
    @Field(1)
    public final boolean result;
    @Field(2)
    public final String errorMessage;
    @Field(3)
    public final int statusValue;
    @Field(4)
    public final int firstPartyStatusValue;

    @Constructor
    public GoogleCertificatesLookupResponse(@Param(1) boolean result, @Param(2) String errorMessage, @Param(3) int statusValue, @Param(4) int firstPartyStatusValue) {
        this.result = result;
        this.errorMessage = errorMessage;
        this.statusValue = statusValue;
        this.firstPartyStatusValue = firstPartyStatusValue;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        CREATOR.writeToParcel(this, out, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GoogleCertificatesLookupResponse> CREATOR = findCreator(GoogleCertificatesLookupResponse.class);
}
