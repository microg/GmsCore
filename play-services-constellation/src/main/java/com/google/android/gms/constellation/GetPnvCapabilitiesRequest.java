/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation;

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
public class GetPnvCapabilitiesRequest extends AbstractSafeParcelable {

    @Field(1)
    @Nullable
    public String policyId;

    @Field(value = 2, subClass = String.class)
    @Nullable
    public List<String> list1;

    @Field(value = 3, subClass = String.class)
    @Nullable
    public List<String> list2;

    private GetPnvCapabilitiesRequest() {
    }

    @Constructor
    public GetPnvCapabilitiesRequest(
            @Param(1) @Nullable String policyId,
            @Param(2) @Nullable List<String> list1,
            @Param(3) @Nullable List<String> list2) {
        this.policyId = policyId;
        this.list1 = list1;
        this.list2 = list2;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GetPnvCapabilitiesRequest> CREATOR = findCreator(GetPnvCapabilitiesRequest.class);
}
