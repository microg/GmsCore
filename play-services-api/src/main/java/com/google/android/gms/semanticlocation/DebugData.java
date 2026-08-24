/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.semanticlocation;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.utils.ToStringHelper;

import java.util.List;

@SafeParcelable.Class
public class DebugData extends AbstractSafeParcelable {
    @Field(1)
    public final List<InputSignals> inputSignals;
    @Field(2)
    public final int versionNumber;
    @Field(3)
    public final List<SemanticSegment> segments;
    @Field(4)
    boolean b4;
    @Field(5)
    List<String> sl5;

    @Constructor
    public DebugData(@Param(1) List<InputSignals> inputSignals, @Param(2) int versionNumber, @Param(3) List<SemanticSegment> segments) {
        this.inputSignals = inputSignals;
        this.versionNumber = versionNumber;
        this.segments = segments;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("DebugData")
                .field("inputSignals", inputSignals)
                .field("versionNumber", versionNumber)
                .field("segments", segments)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<DebugData> CREATOR = findCreator(DebugData.class);
}
