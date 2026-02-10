/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation;

import android.os.Bundle;
import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;

import java.util.List;

@Hide
@SafeParcelable.Class
public class VerifyPhoneNumberRequest extends AbstractSafeParcelable {

    @Field(1)
    @Nullable
    public String upiPolicyId;

    @Field(2)
    public long timestamp;

    @Field(3)
    @Nullable
    public IdTokenRequest idTokenRequest;

    @Field(4)
    @Nullable
    public Bundle extras;

    @Field(value = 5, subClass = ImsiRequest.class)
    @Nullable
    public List<ImsiRequest> imsis;

    @Field(6)
    public boolean flag;

    @Field(7)
    public int withLocalRead;

    @Field(value = 8, subClass = String.class)
    @Nullable
    public List<String> unknownList;

    private VerifyPhoneNumberRequest() {
    }

    @Constructor
    public VerifyPhoneNumberRequest(
            @Param(1) @Nullable String upiPolicyId,
            @Param(2) long timestamp,
            @Param(3) @Nullable IdTokenRequest idTokenRequest,
            @Param(4) @Nullable Bundle extras,
            @Param(5) @Nullable List<ImsiRequest> imsis,
            @Param(6) boolean flag,
            @Param(7) int withLocalRead,
            @Param(8) @Nullable List<String> unknownList) {
        this.upiPolicyId = upiPolicyId;
        this.timestamp = timestamp;
        this.idTokenRequest = idTokenRequest;
        this.extras = extras;
        this.imsis = imsis;
        this.flag = flag;
        this.withLocalRead = withLocalRead;
        this.unknownList = unknownList;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<VerifyPhoneNumberRequest> CREATOR = findCreator(VerifyPhoneNumberRequest.class);
}
