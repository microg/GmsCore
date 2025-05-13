/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.api;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class ApiMetadata extends AbstractSafeParcelable {

    private static final ApiMetadata DEFAULT = new ApiMetadata(null);

    @Field(1)
    public ComplianceOptions complianceOptions;

    public ApiMetadata() {
    }

    @Constructor
    public ApiMetadata(@Param(1) ComplianceOptions complianceOptions) {
        this.complianceOptions = complianceOptions;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ApiMetadata> CREATOR = new ApiMetadataCreator();

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("ApiMetadata").field("complianceOptions", complianceOptions).end();
    }

    private static class ApiMetadataCreator implements SafeParcelableCreatorAndWriter<ApiMetadata> {

        @Override
        public ApiMetadata createFromParcel(Parcel parcel) {
            int dataPosition = parcel.dataPosition();
            int readInt = parcel.readInt();
            if (readInt == -204102970) {
                return findCreator(ApiMetadata.class).createFromParcel(parcel);
            }
            parcel.setDataPosition(dataPosition - 4);
            return ApiMetadata.DEFAULT;
        }

        @Override
        public ApiMetadata[] newArray(int size) {
            return new ApiMetadata[size];
        }

        @Override
        public void writeToParcel(ApiMetadata object, Parcel parcel, int flags) {
            parcel.writeInt(-204102970);
            findCreator(ApiMetadata.class).writeToParcel(object, parcel, flags);
        }
    }
}
