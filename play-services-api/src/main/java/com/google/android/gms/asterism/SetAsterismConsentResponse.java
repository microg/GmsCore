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
public class SetAsterismConsentResponse extends AbstractSafeParcelable {
    @Field(1)
    public int requestCode;
    @Field(2)
    public String iidToken;
    @Field(3)
    public String gaiaToken;

    @Constructor
    public SetAsterismConsentResponse(@Param(1) int requestCode, @Param(2) String iidToken, @Param(3) String gaiaToken) {
        this.requestCode = requestCode;
        this.iidToken = iidToken;
        this.gaiaToken = gaiaToken;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SetAsterismConsentResponse> CREATOR = findCreator(SetAsterismConsentResponse.class);
}
