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

@SafeParcelable.Class
public class SemanticLocationEvent extends AbstractSafeParcelable {
    @Field(1)
    public final long time;
    @Field(2)
    @Deprecated
    String s2;
    @Field(3)
    public final int eventType;
    @Field(4)
    public final PlaceEnterEvent placeEnterEvent;
    @Field(5)
    public final PlaceExitEvent placeExitEvent;
    @Field(6)
    public final PlaceOngoingEvent placeOngoingEvent;
    @Field(7)
    public final ActivityStartEvent activityStartEvent;
    @Field(8)
    public final ActivityEndEvent activityEndEvent;
    @Field(9)
    public final ActivityOngoingEvent activityOngoingEvent;

    @Constructor
    public SemanticLocationEvent(@Param(1) long time, @Param(3) int eventType, @Param(4) PlaceEnterEvent placeEnterEvent, @Param(5) PlaceExitEvent placeExitEvent, @Param(6) PlaceOngoingEvent placeOngoingEvent, @Param(7) ActivityStartEvent activityStartEvent, @Param(8) ActivityEndEvent activityEndEvent, @Param(9) ActivityOngoingEvent activityOngoingEvent) {
        this.time = time;
        this.eventType = eventType;
        this.placeEnterEvent = placeEnterEvent;
        this.placeExitEvent = placeExitEvent;
        this.placeOngoingEvent = placeOngoingEvent;
        this.activityStartEvent = activityStartEvent;
        this.activityEndEvent = activityEndEvent;
        this.activityOngoingEvent = activityOngoingEvent;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("SemanticLocationEvent")
                .field("time", time)
                .field("eventType", eventType)
                .field("placeEnterEvent", placeEnterEvent)
                .field("placeExitEvent", placeExitEvent)
                .field("placeOngoingEvent", placeOngoingEvent)
                .field("activityStartEvent", activityStartEvent)
                .field("activityEndEvent", activityEndEvent)
                .field("activityOngoingEvent", activityOngoingEvent)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SemanticLocationEvent> CREATOR = findCreator(SemanticLocationEvent.class);
}
