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

import org.microg.gms.common.Hide;
import org.microg.gms.utils.ToStringHelper;

@Hide
@SafeParcelable.Class
public class ApiMetadata extends AbstractSafeParcelable {

    public static final ApiMetadata DEFAULT = new ApiMetadata(null);
    public static final ApiMetadata SKIP = new ApiMetadata(true);

    @Field(1)
    public final ComplianceOptions complianceOptions;

    public final boolean skip;

    @Constructor
    public ApiMetadata(@Param(1) ComplianceOptions complianceOptions) {
        this.complianceOptions = complianceOptions;
        this.skip = false;
    }

    private ApiMetadata(boolean skip) {
        this.complianceOptions = null;
        this.skip = skip;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ApiMetadata> CREATOR = new ApiMetadataCreator();
    private static final SafeParcelableCreatorAndWriter<ApiMetadata> ORIGINAL_CREATOR = findCreator(ApiMetadata.class);

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("ApiMetadata").field("complianceOptions", complianceOptions).end();
    }

    private static class ApiMetadataCreator implements SafeParcelableCreatorAndWriter<ApiMetadata> {
        private static final int METADATA_PRESENT_MAGIC = -204102970;

        @Override
        public ApiMetadata createFromParcel(Parcel parcel) {
            int dataPosition = parcel.dataPosition();
            if (parcel.readInt() != METADATA_PRESENT_MAGIC) {
                parcel.setDataPosition(dataPosition - 4);
                return ApiMetadata.DEFAULT;
            }
            return ORIGINAL_CREATOR.createFromParcel(parcel);
        }

        @Override
        public ApiMetadata[] newArray(int size) {
            return new ApiMetadata[size];
        }

        @Override
        public void writeToParcel(ApiMetadata object, Parcel parcel, int flags) {
            if (object.skip) {
                parcel.setDataPosition(parcel.dataPosition() - 4);
                parcel.setDataSize(parcel.dataPosition() - 4);
                return;
            }
            parcel.writeInt(METADATA_PRESENT_MAGIC);
            ORIGINAL_CREATOR.writeToParcel(object, parcel, flags);
        }
    }
}
