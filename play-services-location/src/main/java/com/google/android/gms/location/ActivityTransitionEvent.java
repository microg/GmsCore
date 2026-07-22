/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import androidx.annotation.NonNull;
import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.Objects;

/**
 * Represents an activity transition event, for example start to walk, stop running etc.
 */
@PublicApi
public class ActivityTransitionEvent extends AutoSafeParcelable {
    @Field(1)
    private int activityType;
    @Field(2)
    private int transitionType;
    @Field(3)
    private long elapsedRealtimeNanos;

    private ActivityTransitionEvent() {
    }

    /**
     * Creates an activity transition event.
     *
     * @param activityType         the type of the activity of this transition
     * @param transitionType       the type of transition
     * @param elapsedRealtimeNanos the elapsed realtime when this transition happened
     */
    public ActivityTransitionEvent(int activityType, int transitionType, long elapsedRealtimeNanos) {
        this.activityType = activityType;
        this.transitionType = transitionType;
        this.elapsedRealtimeNanos = elapsedRealtimeNanos;
    }

    /**
     * Gets the type of the activity of this transition. It's one of activity types defined in {@link DetectedActivity}.
     */
    public int getActivityType() {
        return activityType;
    }

    /**
     * Gets the elapsed realtime when this transition happened. Note that the event may happen in the past which means this timestamp may be much smaller than
     * the current time.
     */
    public long getElapsedRealTimeNanos() {
        return elapsedRealtimeNanos;
    }

    /**
     * Gets the type of the transition. It's one of the transition types defined in {@link ActivityTransition}.
     */
    public int getTransitionType() {
        return transitionType;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(new Object[]{activityType, transitionType, elapsedRealtimeNanos});
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ActivityTransitionEvent)) return false;

        ActivityTransitionEvent that = (ActivityTransitionEvent) o;

        if (activityType != that.activityType) return false;
        if (transitionType != that.transitionType) return false;
        return elapsedRealtimeNanos == that.elapsedRealtimeNanos;
    }

    @NonNull
    @Override
    public String toString() {
        return "ActivityType " + activityType + " TransitionType " + transitionType + " ElapsedRealTimeNanos " + elapsedRealtimeNanos;
    }

    public static final Creator<ActivityTransitionEvent> CREATOR = new AutoCreator<>(ActivityTransitionEvent.class);
}
