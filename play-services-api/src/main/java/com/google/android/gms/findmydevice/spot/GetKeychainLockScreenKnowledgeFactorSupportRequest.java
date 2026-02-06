/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.findmydevice.spot;

import android.accounts.Account;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class GetKeychainLockScreenKnowledgeFactorSupportRequest extends AbstractSafeParcelable {
    public static final SafeParcelableCreatorAndWriter<GetKeychainLockScreenKnowledgeFactorSupportRequest> CREATOR = findCreator(GetKeychainLockScreenKnowledgeFactorSupportRequest.class);

    @Field(1)
    public Account account;
    @Field(2)
    public boolean isSupport;

    @Constructor
    public GetKeychainLockScreenKnowledgeFactorSupportRequest() {

    }

    @Constructor
    public GetKeychainLockScreenKnowledgeFactorSupportRequest(@Param(1) Account account, @Param(2) boolean isSupport) {
        this.account = account;
        this.isSupport = isSupport;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("GetKeychainLockScreenKnowledgeFactorSupportRequest")
                .field("account", account)
                .field("isSupport", isSupport)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}