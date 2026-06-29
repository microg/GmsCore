/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.asterism;

import android.os.Bundle;
import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class SetAsterismConsentRequest extends AbstractSafeParcelable {
    @Field(1)
    public int requestCode;
    @Field(2)
    public int asterClientType;
    @Field(4)
    public long timestamp;
    @Field(5)
    public int consentSource;
    @Field(6)
    public Bundle extras;
    @Field(7)
    public int consentVariant;

    @Constructor
    public SetAsterismConsentRequest(@Param(1) int requestCode, @Param(2) int asterClientType,
            @Param(4) long timestamp, @Param(5) int consentSource, @Param(6) Bundle extras,
            @Param(7) int consentVariant) {
        this.requestCode = requestCode;
        this.asterClientType = asterClientType;
        this.timestamp = timestamp;
        this.consentSource = consentSource;
        this.extras = extras;
        this.consentVariant = consentVariant;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SetAsterismConsentRequest> CREATOR = findCreator(SetAsterismConsentRequest.class);
}
