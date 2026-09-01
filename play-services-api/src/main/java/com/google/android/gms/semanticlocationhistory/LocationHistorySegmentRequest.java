/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.semanticlocationhistory;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;

import java.util.List;

@SafeParcelable.Class
public class LocationHistorySegmentRequest extends AbstractSafeParcelable {

    @Field(1)
    public final List<LookupParameters> parameters;
    @Field(2)
    public final FieldMask fieldMask;
    @Field(3)
    public final boolean skipFlush;

    @Constructor
    public LocationHistorySegmentRequest(@Param(1) List<LookupParameters> parameters, @Param(2) FieldMask fieldMask, @Param(3) boolean skipFlush) {
        this.parameters = parameters;
        this.fieldMask = fieldMask;
        this.skipFlush = skipFlush;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<LocationHistorySegmentRequest> CREATOR = findCreator(LocationHistorySegmentRequest.class);

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("LocationHistorySegmentRequest")
                .field("parameters", parameters)
                .field("fieldMask", fieldMask)
                .field("skipFlush", skipFlush)
                .end();
    }
}
