/*
 * Copyright (C) 2013-2026 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.constellation;

import android.os.Bundle;
import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.List;

@SafeParcelable.Class
public class VerifyPhoneNumberRequest extends AbstractSafeParcelable {
    public static SafeParcelableCreatorAndWriter<VerifyPhoneNumberRequest> CREATOR = findCreator(VerifyPhoneNumberRequest.class);

    @SafeParcelable.Field(1)
    @Nullable
    public final String policyId;

    @SafeParcelable.Field(2)
    public final long timeout;

    @SafeParcelable.Field(3)
    @Nullable
    public final Bundle idTokenRequest;

    @SafeParcelable.Field(4)
    @Nullable
    public final Bundle extras;

    @SafeParcelable.Field(5)
    @Nullable
    public final List<Bundle> targetedSims;

    @SafeParcelable.Field(6)
    public final boolean includeUnverified;

    @SafeParcelable.Field(7)
    public final int apiVersion;

    @SafeParcelable.Field(8)
    @Nullable
    public final List<Integer> verificationMethodsValues;

    @SafeParcelable.Constructor
    public VerifyPhoneNumberRequest(
            @SafeParcelable.Param(1) @Nullable String policyId,
            @SafeParcelable.Param(2) long timeout,
            @SafeParcelable.Param(3) @Nullable Bundle idTokenRequest,
            @SafeParcelable.Param(4) @Nullable Bundle extras,
            @SafeParcelable.Param(5) @Nullable List<Bundle> targetedSims,
            @SafeParcelable.Param(6) boolean includeUnverified,
            @SafeParcelable.Param(7) int apiVersion,
            @SafeParcelable.Param(8) @Nullable List<Integer> verificationMethodsValues) {
        this.policyId = policyId;
        this.timeout = timeout;
        this.idTokenRequest = idTokenRequest;
        this.extras = extras;
        this.targetedSims = targetedSims;
        this.includeUnverified = includeUnverified;
        this.apiVersion = apiVersion;
        this.verificationMethodsValues = verificationMethodsValues;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
