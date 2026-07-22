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
public class SemanticLocationState extends AbstractSafeParcelable {
    @Field(1)
    public final List<SemanticLocationEvent> events;
    @Field(2)
    public final long timesamp;
    @Field(3)
    public final DebugData debugData;
    @Field(4)
    public final String stateId;

    @Constructor
    public SemanticLocationState(@Param(1) List<SemanticLocationEvent> events, @Param(2) long timesamp, @Param(3) DebugData debugData, @Param(4) String stateId) {
        this.events = events;
        this.timesamp = timesamp;
        this.debugData = debugData;
        this.stateId = stateId;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("SemanticLocationState")
                .field("events", events)
                .field("timestamp", timesamp)
                .field("debugData", debugData)
                .field("stateId", stateId)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SemanticLocationState> CREATOR = findCreator(SemanticLocationState.class);
}
