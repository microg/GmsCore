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
public class SyncOwnerKeyRequest extends AbstractSafeParcelable {
    public static final SafeParcelableCreatorAndWriter<SyncOwnerKeyRequest> CREATOR = findCreator(SyncOwnerKeyRequest.class);

    @Field(1)
    public Account account;

    @Constructor
    public SyncOwnerKeyRequest() {
    }

    @Constructor
    public SyncOwnerKeyRequest(@Param(1) Account account) {
        this.account = account;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("SyncOwnerKeyRequest")
                .field("account", account)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}