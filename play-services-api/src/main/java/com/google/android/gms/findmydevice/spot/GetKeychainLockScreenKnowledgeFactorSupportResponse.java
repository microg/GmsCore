/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.findmydevice.spot;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class GetKeychainLockScreenKnowledgeFactorSupportResponse extends AbstractSafeParcelable {
    @Field(1)
    public boolean hasKeychainSupport;

    @Field(2)
    public boolean hasLockScreenSupport;

    @Field(3)
    public Boolean keychainStatus;

    @Field(4)
    public Boolean lockScreenStatus;

    @Constructor
    public GetKeychainLockScreenKnowledgeFactorSupportResponse() {
    }

    @Constructor
    public GetKeychainLockScreenKnowledgeFactorSupportResponse(@Param(1) boolean hasKeychainSupport, @Param(2) boolean hasLockScreenSupport, @Param(3) Boolean keychainStatus, @Param(4) Boolean lockScreenStatus) {
        this.hasKeychainSupport = hasKeychainSupport;
        this.hasLockScreenSupport = hasLockScreenSupport;
        this.keychainStatus = keychainStatus;
        this.lockScreenStatus = lockScreenStatus;
    }

    public static final SafeParcelableCreatorAndWriter<GetKeychainLockScreenKnowledgeFactorSupportResponse> CREATOR = findCreator(GetKeychainLockScreenKnowledgeFactorSupportResponse.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
