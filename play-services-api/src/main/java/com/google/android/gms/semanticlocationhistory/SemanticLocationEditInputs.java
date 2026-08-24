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
import com.google.android.gms.semanticlocation.PlaceCandidate;

@SafeParcelable.Class
public class SemanticLocationEditInputs extends AbstractSafeParcelable {
    @Field(1)
    public final int editType;
    @Field(2)
    public final long timestamp;
    @Field(3)
    public final PlaceCandidate.Identifier identifier;
    @Field(4)
    String s4;

    @Constructor
    public SemanticLocationEditInputs(@Param(1) int editType, @Param(2) long timestamp, @Param(3) PlaceCandidate.Identifier identifier) {
        this.editType = editType;
        this.timestamp = timestamp;
        this.identifier = identifier;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SemanticLocationEditInputs> CREATOR = findCreator(SemanticLocationEditInputs.class);
}
