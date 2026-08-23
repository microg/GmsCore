/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.asterism;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class GetAsterismConsentRequest extends AbstractSafeParcelable {
    @Field(1)
    public String packageName;

    @Field(2)
    public String accountName;

    @Field(3)
    public int consentType;

    @Field(4)
    public byte[] requestToken;

    public GetAsterismConsentRequest() {
    }

    public GetAsterismConsentRequest(String packageName, String accountName, int consentType, byte[] requestToken) {
        this.packageName = packageName;
        this.accountName = accountName;
        this.consentType = consentType;
        this.requestToken = requestToken;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("GetAsterismConsentRequest")
                .field("packageName", packageName)
                .field("accountName", accountName)
                .field("consentType", consentType)
                .field("requestToken", requestToken)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GetAsterismConsentRequest> CREATOR = findCreator(GetAsterismConsentRequest.class);
}
