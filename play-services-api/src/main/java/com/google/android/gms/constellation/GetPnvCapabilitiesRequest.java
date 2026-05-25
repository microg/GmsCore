/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.List;

/**
 * Request to get phone number verification (PNV) capabilities.
 *
 * Fields:
 * - field 1: policyId - UPI policy string
 * - field 2: verificationMethods - List of method IDs to check
 * - field 3: subscriptionIds - List of subscription IDs
 */
@SafeParcelable.Class
public class GetPnvCapabilitiesRequest extends AbstractSafeParcelable {
    @Field(1)
    public String policyId;
    @Field(2)
    public List<Integer> verificationMethods;
    @Field(3)
    public List<Integer> subscriptionIds;

    @Constructor
    public GetPnvCapabilitiesRequest(
            @Param(1) String policyId,
            @Param(2) List<Integer> verificationMethods,
            @Param(3) List<Integer> subscriptionIds) {
        this.policyId = policyId;
        this.verificationMethods = verificationMethods;
        this.subscriptionIds = subscriptionIds;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GetPnvCapabilitiesRequest> CREATOR = findCreator(GetPnvCapabilitiesRequest.class);
}
