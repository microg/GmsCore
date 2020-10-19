/*
 * Copyright (C) 2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.location

import android.app.PendingIntent
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.PendingResult
import com.google.android.gms.common.api.Status

/**
 * The main entry point for interacting with activity recognition.
 *
 *
 * The methods must be used in conjunction with a GoogleApiClient. E.g.
 * <pre>
 * new GoogleApiClient.Builder(context)
 * .addApi(ActivityRecognition.API)
 * .addConnectionCallbacks(this)
 * .addOnConnectionFailedListener(this)
 * .build()
</pre> *
 */
interface ActivityRecognitionApi {
    /**
     * Removes all activity updates for the specified PendingIntent.
     *
     *
     * Calling this function requires the com.google.android.gms.permission.ACTIVITY_RECOGNITION
     * permission.
     *
     * @param client         An existing GoogleApiClient. It must be connected at the time of this
     * call, which is normally achieved by calling [GoogleApiClient.connect]
     * and waiting for [ConnectionCallbacks.onConnected] to be
     * called.
     * @param callbackIntent the PendingIntent that was used in `#requestActivityUpdates(GoogleApiClient, long, PendingIntent)`
     * or is equal as defined by [Object.equals].
     * @return a PendingResult for the call, check [Status.isSuccess] to determine if it
     * was successful.
     */
    fun removeActivityUpdates(client: GoogleApiClient?, callbackIntent: PendingIntent?): PendingResult<Status?>?

    /**
     * Register for activity recognition updates.
     *
     *
     * The activities are detected by periodically waking up the device and reading short bursts of
     * sensor data. It only makes use of low power sensors in order to keep the power usage to a
     * minimum. For example, it can detect if the user is currently on foot, in a car, on a bicycle
     * or still. See [DetectedActivity] for more details.
     *
     *
     * The activity detection update interval can be controlled with the detectionIntervalMillis
     * parameter. Larger values will result in fewer activity detections while improving battery
     * life. Smaller values will result in more frequent activity detections but will consume more
     * power since the device must be woken up more frequently. `Long.MAX_VALUE` means it only
     * monitors the results requested by other clients without consuming additional power.
     *
     *
     * Activities may be received more frequently than the detectionIntervalMillis parameter if
     * another application has also requested activity updates at a faster rate. It may also receive
     * updates faster when the activity detection service receives a signal that the current
     * activity may change, such as if the device has been still for a long period of time and is
     * then unplugged from a phone charger.
     *
     *
     * Activities may arrive several seconds after the requested detectionIntervalMillis if the
     * activity detection service requires more samples to make a more accurate prediction.
     *
     *
     * To conserve battery, activity reporting may stop when the device is 'STILL' for an extended
     * period of time. It will resume once the device moves again. This only happens on devices that
     * support the Sensor.TYPE_SIGNIFICANT_MOTION hardware.
     *
     *
     * Beginning in API 21, activities may be received less frequently than the
     * detectionIntervalMillis parameter if the device is in power save mode and the screen is off.
     *
     *
     * A common use case is that an application wants to monitor activities in the background and
     * perform an action when a specific activity is detected. To do this without needing a service
     * that is always on in the background consuming resources, detected activities are delivered
     * via an intent. The application specifies a PendingIntent callback (typically an
     * IntentService) which will be called with an intent when activities are detected. The intent
     * recipient can extract the [ActivityRecognitionResult] using [ActivityRecognitionResult.extractResult].
     * See the documentation of [PendingIntent] for more details.
     *
     *
     * Any requests previously registered with [.requestActivityUpdates]
     * that have the same PendingIntent (as defined by [Object.equals]) will be
     * replaced by this request.
     *
     *
     * Calling this function requires the com.google.android.gms.permission.ACTIVITY_RECOGNITION
     * permission.
     *
     * @param client                  An existing GoogleApiClient. It must be connected at the time
     * of this call, which is normally achieved by calling [GoogleApiClient.connect]
     * and waiting for [ConnectionCallbacks.onConnected]
     * to be called.
     * @param detectionIntervalMillis the desired time between activity detections. Larger values
     * will result in fewer activity detections while improving
     * battery life. A value of 0 will result in activity detections
     * at the fastest possible rate.
     * @param callbackIntent          a PendingIntent to be sent for each activity detection.
     * @return a PendingResult for the call, check [Status.isSuccess] to determine if it
     * was successful.
     */
    fun requestActivityUpdates(client: GoogleApiClient?, detectionIntervalMillis: Long, callbackIntent: PendingIntent?): PendingResult<Status?>?
}