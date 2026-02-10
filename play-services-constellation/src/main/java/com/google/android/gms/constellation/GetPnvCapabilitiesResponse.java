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
public class GetPnvCapabilitiesResponse extends AbstractSafeParcelable {

    @Field(value = 1, subClass = String.class)
    @Nullable
    public List<String> capabilities;

    private GetPnvCapabilitiesResponse() {
    }

    @Constructor
    public GetPnvCapabilitiesResponse(@Param(1) @Nullable List<String> capabilities) {
        this.capabilities = capabilities;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GetPnvCapabilitiesResponse> CREATOR = findCreator(GetPnvCapabilitiesResponse.class);
}
