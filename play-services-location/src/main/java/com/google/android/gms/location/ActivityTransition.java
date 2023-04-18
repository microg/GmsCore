/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.microg.safeparcel.AutoSafeParcelable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

/**
 * Represents an activity and the transition of it. For instance start to walk; stop running etc.
 */
public class ActivityTransition extends AutoSafeParcelable {
    /**
     * User enters the given activity.
     */
    public static final int ACTIVITY_TRANSITION_ENTER = 0;
    /**
     * User exits the given activity.
     */
    public static final int ACTIVITY_TRANSITION_EXIT = 1;

    @Field(1)
    private int activityType;
    @Field(2)
    private @SupportedActivityTransition int transitionType;

    private ActivityTransition() {
    }

    private ActivityTransition(int activityType, @SupportedActivityTransition int transitionType) {
        this.activityType = activityType;
        this.transitionType = transitionType;
    }

    /**
     * Gets the type of the activity to be detected.
     */
    public int getActivityType() {
        return activityType;
    }

    /**
     * Gets the interested transition type. It's one of the ACTIVITY_TRANSITION_xxx constants.
     */
    public @SupportedActivityTransition int getTransitionType() {
        return transitionType;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(new Object[]{activityType, transitionType});
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof ActivityTransition && ((ActivityTransition) obj).activityType == activityType && ((ActivityTransition) obj).transitionType == transitionType;
    }

    @NonNull
    @Override
    public String toString() {
        return "ActivityTransition [mActivityType=" + activityType + ", mTransitionType=" + transitionType + "]";
    }

    /**
     * Activity transition constants annotation.
     */
    @Target({ElementType.TYPE_USE})
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ACTIVITY_TRANSITION_ENTER, ACTIVITY_TRANSITION_EXIT})
    public @interface SupportedActivityTransition {
    }

    /**
     * The builder to help create an {@link ActivityTransition} object.
     */
    public static class Builder {
        private int activityType;
        private int transitionType;

        /**
         * Adds an interested transition type.
         *
         * @param transition the interested transition type. It's one of the ACTIVITY_TRANSITION_xxx constants.
         * @return this builder
         */
        public ActivityTransition.Builder setActivityTransition(int transition) {
            this.transitionType = transition;
            return this;
        }

        /**
         * Sets the type of the activity to be detected.
         *
         * @param activityType the type of the activity to be detected. It's one of the constant in {@link DetectedActivity}.
         * @return this builder
         */
        public ActivityTransition.Builder setActivityType(int activityType) {
            this.activityType = activityType;
            return this;
        }

        public ActivityTransition build() {
            return new ActivityTransition(activityType, transitionType);
        }
    }

    public static final Creator<ActivityTransition> CREATOR = new AutoCreator<>(ActivityTransition.class);
}
