/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.wallet.shared;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class BuyFlowConfig extends AbstractSafeParcelable {
    @Field(2)
    public String googleTransactionId;
    @Field(3)
    public ApplicationParameters applicationParameters;
    @Field(4)
    public String callerPackage;
    @Field(5)
    public String buyFlowName;
    @Field(6)
    public String androidPackageName;
    @Field(7)
    public String sessionId;
    @Field(8)
    public int sessionRestoreOption;

    public BuyFlowConfig() {
    }

    @Constructor
    public BuyFlowConfig(@Param(2) String googleTransactionId, @Param(3) ApplicationParameters applicationParameters, @Param(4) String callerPackage,
                         @Param(5) String buyFlowName, @Param(6) String androidPackageName, @Param(7) String sessionId, @Param(8) int sessionRestoreOption) {
        this.googleTransactionId = googleTransactionId;
        this.applicationParameters = applicationParameters;
        this.callerPackage = callerPackage;
        this.buyFlowName = buyFlowName;
        this.androidPackageName = androidPackageName;
        this.sessionId = sessionId;
        this.sessionRestoreOption = sessionRestoreOption;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<BuyFlowConfig> CREATOR = findCreator(BuyFlowConfig.class);
}
