/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.location.internal;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class SetGoogleLocationAccuracyRequest extends AbstractSafeParcelable {
    @Field(1)
    public final boolean networkLocationOptIn;
    @Field(2)
    public final int settingSource;
    @Field(3)
    public final byte[] uiConsentBytes; //Used for Audit reporting, can be converted into proto
    @Field(4)
    public final byte[] auditTokenBytes; //Used for Audit reporting, can be converted into proto

    @Constructor
    public SetGoogleLocationAccuracyRequest(@Param(1) boolean networkLocationOptIn, @Param(2) int settingSource,
                                            @Param(3) byte[] uiConsentBytes, @Param(4) byte[] auditTokenBytes) {
        this.networkLocationOptIn = networkLocationOptIn;
        this.settingSource = settingSource;
        this.uiConsentBytes = uiConsentBytes;
        this.auditTokenBytes = auditTokenBytes;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    @Override
    public String toString() {
        return ToStringHelper.name("SetGoogleLocationAccuracyRequest")
                .field("networkLocationOptIn", networkLocationOptIn)
                .field("settingSource", settingSource)
                .field("uiConsentBytes", uiConsentBytes)
                .field("auditTokenBytes", auditTokenBytes)
                .end();
    }

    public static final SafeParcelableCreatorAndWriter<SetGoogleLocationAccuracyRequest> CREATOR = findCreator(SetGoogleLocationAccuracyRequest.class);
}