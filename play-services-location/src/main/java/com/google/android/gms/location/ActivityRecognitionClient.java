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
     * Activity Recognition Transition API provides an ability for apps to subscribe to activity transitional conditions (enter, exit). For example, a
     * messaging app that wants to build a distraction free driving experiences can ask -- tell me when user has entered the vehicle or exited the vehicle. It
     * doesn't have to worry about user being {@link DetectedActivity#STILL} at the traffic signal, or any other transient activities while in vehicle
     * ({@link DetectedActivity#IN_VEHICLE}), that is, the API will fence around the activity boundaries using Activity Recognition Filtering.
     *
     * @param activityTransitionRequest the interested activity transitions
     * @param pendingIntent             the {@link PendingIntent} used to generate the callback intent when one of the interested transition has happened
     * @return a {@link Task} for apps to check the status of the call. If the task fails, the status code for the
     * failure can be found by examining {@link ApiException#getStatusCode()}.
     */
    Task<Void> requestActivityTransitionUpdates(ActivityTransitionRequest activityTransitionRequest, PendingIntent pendingIntent);

    /**
     * Register for activity recognition updates.
     * <p>
     * The activities are detected by periodically waking up the device and reading short bursts of sensor data. It only makes use of low power sensors in order
     * to keep the power usage to a minimum. For example, it can detect if the user is currently on foot, in a car, on a bicycle or still. See
     * {@link DetectedActivity} for more details.
     *
     * @param detectionIntervalMillis the desired time between activity detections. Larger values will result in fewer activity detections while improving
     *                                battery life. A value of 0 will result in activity detections at the fastest possible rate. Note that a fast rate can
     *                                result in excessive device wakelocks and power consumption.
     * @param callbackIntent          a PendingIntent to be sent for each activity detection.
     * @return a {@link Task} for apps to check the status of the call. If the task fails, the status code for the
     * failure can be found by examining {@link ApiException#getStatusCode()}.
     */
    Task<Void> requestActivityUpdates(long detectionIntervalMillis, PendingIntent callbackIntent);

    /**
     * Registers for detected user sleep time ({@code SleepSegmentEvent}) and/or periodic sleep activity classification results ({@code SleepClassifyEvent})
     * based on the data type specified in {@link SleepSegmentRequest}. It is advised to the apps to re-register after device reboot or app upgrade, from a
     * receiver that handles {@code android.intent.action.BOOT_COMPLETED} and {@code android.intent.action.MY_PACKAGE_REPLACED} events.
     *
     * @param callbackIntent      a PendingIntent to be sent for each sleep segment or classification result
     * @param sleepSegmentRequest a {@link SleepSegmentRequest} that specifies whether to receive both {@code SleepSegmentEvent}s and
     *                            {@code SleepClassifyEvent}s, or {@code SleepSegmentEvent}s only, or {@code SleepClassifyEvent}s only.
     * @return a {@link Task} for apps to check the status of the call. If the task fails, the status code for the
     * failure can be found by examining {@link ApiException#getStatusCode()}.
     */
    Task<Void> requestSleepSegmentUpdates(PendingIntent callbackIntent, SleepSegmentRequest sleepSegmentRequest);
}
