/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParcelUtil;

import java.util.Collections;
import java.util.List;

/**
 * Represents the result of activity transitions.
 */
@PublicApi
public class ActivityTransitionResult extends AutoSafeParcelable {
    private static final String EXTRA = "com.google.android.location.internal.EXTRA_ACTIVITY_TRANSITION_RESULT";

    @Field(value = 1, subClass = ActivityTransitionEvent.class)
    @NonNull
    private List<ActivityTransitionEvent> transitionEvents;
    @Field(2)
    private Bundle extras;

    /**
     * Constructs a result by specifying a list of transition events.
     *
     * @param transitionEvents the transition events
     * @throws NullPointerException     if {@code transitionEvents} is {@code null}
     * @throws IllegalArgumentException if the events in {@code transitionEvents} are not in ascending order of time
     */
    public ActivityTransitionResult(List<ActivityTransitionEvent> transitionEvents) {
        if (transitionEvents == null) throw new NullPointerException("transitionEvents list can't be null.");
        for (int i = 1; i < transitionEvents.size(); i++) {
            if (transitionEvents.get(i).getElapsedRealTimeNanos() < transitionEvents.get(i - 1).getElapsedRealTimeNanos())
                throw new IllegalArgumentException();
        }
        this.transitionEvents = Collections.unmodifiableList(transitionEvents);
    }

    /**
     * Gets all the activity transition events in this result. The events are in ascending order of time, and may include events in the past.
     */
    public List<ActivityTransitionEvent> getTransitionEvents() {
        return transitionEvents;
    }

    /**
     * Extracts the {@link ActivityTransitionResult} from the given {@link Intent}.
     *
     * @param intent the {@link Intent} to extract the result from
     * @return the {@link ActivityTransitionResult} included in the given intent or return {@code null} if no such result is found in the given intent
     */
    public static ActivityTransitionResult extractResult(Intent intent) {
        if (!hasResult(intent)) return null;
        return SafeParcelUtil.fromByteArray(intent.getByteArrayExtra(EXTRA), CREATOR);
    }

    /**
     * Checks if the intent contains an {@link ActivityTransitionResult}.
     */
    public static boolean hasResult(Intent intent) {
        return intent != null && intent.hasExtra(EXTRA);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ActivityTransitionResult that = (ActivityTransitionResult) o;

        return transitionEvents.equals(that.transitionEvents);
    }

    @Override
    public int hashCode() {
        return transitionEvents.hashCode();
    }

    public static final Creator<ActivityTransitionResult> CREATOR = new AutoCreator<>(ActivityTransitionResult.class);
}
