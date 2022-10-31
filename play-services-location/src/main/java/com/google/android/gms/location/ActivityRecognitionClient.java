/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import android.app.PendingIntent;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.HasApiKey;
import com.google.android.gms.tasks.Task;

/**
 * The main entry point for interacting with activity recognition.
 * <p>
 * Activity Recognition provides the following APIs: the <b>Activity Recognition Transition API</b>, the <b>Activity
 * Recognition Sampling API</b>, and the <b>Activity Recognition Sleep API</b>.
 */
public interface ActivityRecognitionClient extends HasApiKey<Api.ApiOptions.NoOptions> {
    /**
     * Removes activity transition updates associated with the given {@code pendingIntent}.
     * <p>
     * To call this function, a different permission is required depending on your Android API level:
     * <ul>
     *     <li>For Android 10 (API level 29) and later: {@code android.permission.ACTIVITY_RECOGNITION} permission</li>
     *     <li>For Android 9 (API level 28) and earlier: {@code com.google.android.gms.permission.ACTIVITY_RECOGNITION} permission</li>
     * </ul>
     *
     * @param pendingIntent the associated {@link PendingIntent} of the activity transition request which is to be removed
     * @return a {@link Task} for apps to check the status of the call. If the task fails, the status code for the
     * failure can be found by examining {@link ApiException#getStatusCode()}.
     */
    Task<Void> removeActivityTransitionUpdates(PendingIntent pendingIntent);

    /**
     * Removes all activity updates for the specified PendingIntent.
     * <p>
     * To call this function, a different permission is required depending on your Android API level:
     * <ul>
     *     <li>For Android 10 (API level 29) and later: {@code android.permission.ACTIVITY_RECOGNITION} permission</li>
     *     <li>For Android 9 (API level 28) and earlier: {@code com.google.android.gms.permission.ACTIVITY_RECOGNITION} permission</li>
     * </ul>
     *
     * @param callbackIntent the PendingIntent that was used in {@link #requestActivityUpdates(long, PendingIntent)} or
     *                       is equal as defined by {@link PendingIntent#equals(Object)}.
     * @return a {@link Task} for apps to check the status of the call. If the task fails, the status code for the
     * failure can be found by examining {@link ApiException#getStatusCode()}.
     */
    Task<Void> removeActivityUpdates(PendingIntent callbackIntent);

    /**
     * Removes all sleep segment detection updates for the specified {@code PendingIntent}.
     *
     * @param callbackIntent the PendingIntent that was used in {@link #requestSleepSegmentUpdates(PendingIntent, SleepSegmentRequest)}
     *                       or is equal as defined by {@link PendingIntent#equals(Object)}.
     * @return a {@link Task} for apps to check the status of the call. If the task fails, the status code for the
     * failure can be found by examining {@link ApiException#getStatusCode()}.
     */
    Task<Void> removeSleepSegmentUpdates(PendingIntent callbackIntent);

    /**
     * @param activityTransitionRequest
     * @param pendingIntent
     * @return a {@link Task} for apps to check the status of the call. If the task fails, the status code for the
     * failure can be found by examining {@link ApiException#getStatusCode()}.
     */
    Task<Void> requestActivityTransitionUpdates(ActivityTransitionRequest activityTransitionRequest, PendingIntent pendingIntent);

    /**
     * @param detectionIntervalMillis
     * @param callbackIntent
     * @return a {@link Task} for apps to check the status of the call. If the task fails, the status code for the
     * failure can be found by examining {@link ApiException#getStatusCode()}.
     */
    Task<Void> requestActivityUpdates(long detectionIntervalMillis, PendingIntent callbackIntent);

    /**
     * @param callbackIntent
     * @param sleepSegmentRequest
     * @return a {@link Task} for apps to check the status of the call. If the task fails, the status code for the
     * failure can be found by examining {@link ApiException#getStatusCode()}.
     */
    Task<Void> requestSleepSegmentUpdates(PendingIntent callbackIntent, SleepSegmentRequest sleepSegmentRequest);
}
