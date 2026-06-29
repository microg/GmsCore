/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.asterism;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class GetAsterismConsentResponse extends AbstractSafeParcelable {
    @Field(1)
    public int requestCode;
    @Field(2)
    public int consentState;
    @Field(3)
    public String iidToken;
    @Field(4)
    public String gaiaToken;
    @Field(5)
    public int consentVersion;

    @Constructor
    public GetAsterismConsentResponse(@Param(1) int requestCode, @Param(2) int consentState,
            @Param(3) String iidToken, @Param(4) String gaiaToken, @Param(5) int consentVersion) {
        this.requestCode = requestCode;
        this.consentState = consentState;
        this.iidToken = iidToken;
        this.gaiaToken = gaiaToken;
        this.consentVersion = consentVersion;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GetAsterismConsentResponse> CREATOR = findCreator(GetAsterismConsentResponse.class);
}
