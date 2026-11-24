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
 * Response containing PNV capabilities.
 *
 * Contains a list of SimCapability objects describing what verification
 * methods are available for each SIM.
 */
@SafeParcelable.Class
public class GetPnvCapabilitiesResponse extends AbstractSafeParcelable {
    @Field(1)
    public List<SimCapability> capabilities;

    @Constructor
    public GetPnvCapabilitiesResponse(@Param(1) List<SimCapability> capabilities) {
        this.capabilities = capabilities;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GetPnvCapabilitiesResponse> CREATOR = findCreator(GetPnvCapabilitiesResponse.class);
}
