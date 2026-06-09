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
public class GetAsterismConsentRequest extends AbstractSafeParcelable {
    @Field(1)
    public int requestCode;
    @Field(2)
    public int asterClientType;

    @Constructor
    public GetAsterismConsentRequest(@Param(1) int requestCode, @Param(2) int asterClientType) {
        this.requestCode = requestCode;
        this.asterClientType = asterClientType;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GetAsterismConsentRequest> CREATOR = findCreator(GetAsterismConsentRequest.class);
}
