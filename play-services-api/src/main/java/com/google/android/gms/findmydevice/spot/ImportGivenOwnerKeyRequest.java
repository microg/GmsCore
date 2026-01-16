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
public class ImportGivenOwnerKeyRequest extends AbstractSafeParcelable {
    public static final SafeParcelableCreatorAndWriter<ImportGivenOwnerKeyRequest> CREATOR = findCreator(ImportGivenOwnerKeyRequest.class);

    @Field(1)
    public Account account;

    @Field(2)
    public int key;

    @Constructor
    public ImportGivenOwnerKeyRequest() {
    }

    @Constructor
    public ImportGivenOwnerKeyRequest(@Param(1) Account account, @Param(2) int key) {
        this.account = account;
        this.key = key;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("ImportGivenOwnerKeyRequest")
                .field("account", account)
                .field("key", key)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}