/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
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
 * Result of an activity recognition.
 * <p>
 * It contains a list of activities that a user may have been doing at a particular time. The activities are sorted by
 * the most probable activity first. A confidence is associated with each activity which indicates how likely that
 * activity is.
 * <p>
 * {@link #getMostProbableActivity()} will return the most probable activity of the user at the time that activity
 * recognition was run.
 */
@PublicApi
public class ActivityRecognitionResult extends AutoSafeParcelable {
    public static final Creator<ActivityRecognitionResult> CREATOR = new AutoCreator<>(ActivityRecognitionResult.class);

    @PublicApi(exclude = true)
    public static final String EXTRA_ACTIVITY_RESULT = "com.google.android.location.internal.EXTRA_ACTIVITY_RESULT";
    @PublicApi(exclude = true)
    public static final String EXTRA_ACTIVITY_RESULT_LIST = "com.google.android.location.internal.EXTRA_ACTIVITY_RESULT_LIST";

    @Field(1000)
    private int versionCode = 2;
    @Field(value = 1, subClass = DetectedActivity.class)
    private List<DetectedActivity> probableActivities;
    @Field(2)
    private long time;
    @Field(3)
    private long elapsedRealtimeMillis;
    @Field(5)
    private Bundle extras;

    private ActivityRecognitionResult() {

    }

    /**
     * Constructs an ActivityRecognitionResult.
     *
     * @param probableActivities    the activities that were detected, sorted by confidence (most probable first).
     * @param time                  the UTC time of this detection, in milliseconds since January 1, 1970.
     * @param elapsedRealtimeMillis milliseconds since boot
     */
    public ActivityRecognitionResult(List<DetectedActivity> probableActivities, long time, long elapsedRealtimeMillis) {
        this.probableActivities = probableActivities;
        this.time = time;
        this.elapsedRealtimeMillis = elapsedRealtimeMillis;
    }

    /**
     * Constructs an ActivityRecognitionResult from a single activity.
     *
     * @param mostProbableActivity  the most probable activity of the device.
     * @param time                  the UTC time of this detection, in milliseconds since January 1, 1970.
     * @param elapsedRealtimeMillis milliseconds since boot
     */
    public ActivityRecognitionResult(DetectedActivity mostProbableActivity, long time, long elapsedRealtimeMillis) {
        this(Collections.singletonList(mostProbableActivity), time, elapsedRealtimeMillis);
    }

    /**
     * Extracts the ActivityRecognitionResult from an Intent.
     * <p>
     * This is a utility function which extracts the ActivityRecognitionResult from the extras of an Intent that was
     * sent from the activity detection service.
     *
     * @return an ActivityRecognitionResult, or {@code null} if the intent doesn't contain an ActivityRecognitionResult.
     */
    public static ActivityRecognitionResult extractResult(Intent intent) {
        if (intent.hasExtra(EXTRA_ACTIVITY_RESULT_LIST)) {
            intent.setExtrasClassLoader(ActivityRecognitionResult.class.getClassLoader());
            List<ActivityRecognitionResult> list = intent.getParcelableArrayListExtra(EXTRA_ACTIVITY_RESULT_LIST);
            if (list != null && !list.isEmpty())
                return list.get(list.size() - 1);
        }
        if (intent.hasExtra(EXTRA_ACTIVITY_RESULT)) {
            Bundle extras = intent.getExtras();
            extras.setClassLoader(ActivityRecognitionResult.class.getClassLoader());
            Object res = extras.get(EXTRA_ACTIVITY_RESULT);
            if (res instanceof ActivityRecognitionResult)
                return (ActivityRecognitionResult) res;
            if (res instanceof byte[])
                return SafeParcelUtil.fromByteArray((byte[]) res, CREATOR);
        }
        return null;
    }

    /**
     * Returns the confidence of the given activity type.
     */
    public int getActivityConfidence(int activityType) {
        for (DetectedActivity activity : probableActivities) {
            if (activity.getType() == activityType)
                return activity.getConfidence();
        }
        return 0;
    }

    /**
     * Returns the elapsed real time of this detection in milliseconds since boot, including time spent in sleep as
     * obtained by SystemClock.elapsedRealtime().
     */
    public long getElapsedRealtimeMillis() {
        return elapsedRealtimeMillis;
    }

    /**
     * Returns the most probable activity of the user.
     */
    public DetectedActivity getMostProbableActivity() {
        return probableActivities.get(0);
    }

    /**
     * Returns the list of activities that were detected with the confidence value associated with each activity.
     * The activities are sorted by most probable activity first.
     * <p>
     * The sum of the confidences of all detected activities this method returns does not have to be <= 100 since some
     * activities are not mutually exclusive (for example, you can be walking while in a bus) and some activities are
     * hierarchical (ON_FOOT is a generalization of WALKING and RUNNING).
     */
    public List<DetectedActivity> getProbableActivities() {
        return probableActivities;
    }

    /**
     * Returns the UTC time of this detection, in milliseconds since January 1, 1970.
     */
    public long getTime() {
        return time;
    }

    /**
     * Returns true if an Intent contains an ActivityRecognitionResult.
     * <p>
     * This is a utility function that can be called from inside an intent receiver to make sure the received intent is
     * from activity recognition.
     *
     * @return true if the intent contains an ActivityRecognitionResult, false otherwise or the given intent is null
     */
    public static boolean hasResult(Intent intent) {
        if (intent == null) return false;
        if (intent.hasExtra(EXTRA_ACTIVITY_RESULT)) return true;
        intent.setExtrasClassLoader(ActivityRecognitionResult.class.getClassLoader());
        List<ActivityRecognitionResult> list = intent.getParcelableArrayListExtra(EXTRA_ACTIVITY_RESULT_LIST);
        return list != null && !list.isEmpty();
    }

    @NonNull
    public String toString() {
        return "ActivityRecognitionResult [probableActivities=" + probableActivities + ", timeMillis" + time + ", elapsedRealtimeMillis=" + elapsedRealtimeMillis + "]";
    }
}
