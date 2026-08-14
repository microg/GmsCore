/*
 * SPDX-FileCopyrightText: 2017 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import android.app.PendingIntent;
import android.os.Bundle;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;

/**
 * The main entry point for interacting with activity recognition.
 * <p>
 * The methods must be used in conjunction with a GoogleApiClient. E.g.
 * <pre>
 *  new GoogleApiClient.Builder(context)
 *          .addApi(ActivityRecognition.API)
 *          .addConnectionCallbacks(this)
 *          .addOnConnectionFailedListener(this)
 *          .build()
 * </pre>
 *
 * @deprecated Use the GoogleApi-based API {@link ActivityRecognitionClient} instead.
 */
public interface ActivityRecognitionApi {
    /**
     * Removes all activity updates for the specified PendingIntent.
     * <p>
     * Calling this function requires the com.google.android.gms.permission.ACTIVITY_RECOGNITION
     * permission.
     *
     * @param client         An existing GoogleApiClient. It must be connected at the time of this
     *                       call, which is normally achieved by calling {@link GoogleApiClient#connect()}
     *                       and waiting for {@link ConnectionCallbacks#onConnected(Bundle)} to be
     *                       called.
     * @param callbackIntent the PendingIntent that was used in {@code #requestActivityUpdates(GoogleApiClient, long, PendingIntent)}
     *                       or is equal as defined by {@link Object#equals(Object)}.
     * @return a PendingResult for the call, check {@link Status#isSuccess()} to determine if it
     * was successful.
     */
    PendingResult<Status> removeActivityUpdates(GoogleApiClient client, PendingIntent callbackIntent);

    /**
     * Register for activity recognition updates.
     * <p>
     * The activities are detected by periodically waking up the device and reading short bursts of
     * sensor data. It only makes use of low power sensors in order to keep the power usage to a
     * minimum. For example, it can detect if the user is currently on foot, in a car, on a bicycle
     * or still. See {@link DetectedActivity} for more details.
     * <p>
     * The activity detection update interval can be controlled with the detectionIntervalMillis
     * parameter. Larger values will result in fewer activity detections while improving battery
     * life. Smaller values will result in more frequent activity detections but will consume more
     * power since the device must be woken up more frequently. {@code Long.MAX_VALUE} means it only
     * monitors the results requested by other clients without consuming additional power.
     * <p>
     * Activities may be received more frequently than the detectionIntervalMillis parameter if
     * another application has also requested activity updates at a faster rate. It may also receive
     * updates faster when the activity detection service receives a signal that the current
     * activity may change, such as if the device has been still for a long period of time and is
     * then unplugged from a phone charger.
     * <p>
     * Activities may arrive several seconds after the requested detectionIntervalMillis if the
     * activity detection service requires more samples to make a more accurate prediction.
     * <p>
     * To conserve battery, activity reporting may stop when the device is 'STILL' for an extended
     * period of time. It will resume once the device moves again. This only happens on devices that
     * support the Sensor.TYPE_SIGNIFICANT_MOTION hardware.
     * <p>
     * Beginning in API 21, activities may be received less frequently than the
     * detectionIntervalMillis parameter if the device is in power save mode and the screen is off.
     * <p>
     * A common use case is that an application wants to monitor activities in the background and
     * perform an action when a specific activity is detected. To do this without needing a service
     * that is always on in the background consuming resources, detected activities are delivered
     * via an intent. The application specifies a PendingIntent callback (typically an
     * IntentService) which will be called with an intent when activities are detected. The intent
     * recipient can extract the {@link ActivityRecognitionResult} using {@link ActivityRecognitionResult#extractResult(android.content.Intent)}.
     * See the documentation of {@link PendingIntent} for more details.
     * <p>
     * Any requests previously registered with {@link #requestActivityUpdates(GoogleApiClient, long, PendingIntent)}
     * that have the same PendingIntent (as defined by {@link Object#equals(Object)}) will be
     * replaced by this request.
     * <p>
     * Calling this function requires the com.google.android.gms.permission.ACTIVITY_RECOGNITION
     * permission.
     *
     * @param client                  An existing GoogleApiClient. It must be connected at the time
     *                                of this call, which is normally achieved by calling {@link GoogleApiClient#connect()}
     *                                and waiting for {@link ConnectionCallbacks#onConnected(Bundle)}
     *                                to be called.
     * @param detectionIntervalMillis the desired time between activity detections. Larger values
     *                                will result in fewer activity detections while improving
     *                                battery life. A value of 0 will result in activity detections
     *                                at the fastest possible rate.
     * @param callbackIntent          a PendingIntent to be sent for each activity detection.
     * @return a PendingResult for the call, check {@link Status#isSuccess()} to determine if it
     * was successful.
     */
    PendingResult<Status> requestActivityUpdates(GoogleApiClient client, long detectionIntervalMillis, PendingIntent callbackIntent);
}
