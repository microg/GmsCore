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

package com.google.android.gms.location;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

/**
 * The detected activity of the device with an an associated confidence. See ActivityRecognitionApi
 * for details on how to obtain a DetectedActivity.
 */
@PublicApi
public class DetectedActivity extends AutoSafeParcelable {

    /**
     * The device is in a vehicle, such as a car.
     */
    public static final int IN_VEHICLE = 0;

    /**
     * The device is on a bicycle.
     */
    public static final int ON_BICYCLE = 1;

    /**
     * The device is on a user who is walking or running.
     */
    public static final int ON_FOOT = 2;

    /**
     * The device is on a user who is running. This is a sub-activity of ON_FOOT.
     */
    public static final int RUNNING = 8;

    /**
     * The device is still (not moving).
     */
    public static final int STILL = 3;

    /**
     * The device angle relative to gravity changed significantly. This often occurs when a device
     * is picked up from a desk or a user who is sitting stands up.
     */
    public static final int TILTING = 5;

    /**
     * Unable to detect the current activity.
     */
    public static final int UNKNOWN = 4;

    /**
     * The device is on a user who is walking. This is a sub-activity of ON_FOOT.
     */
    public static final int WALKING = 7;

    @SafeParceled(1000)
    private int versionCode = 1;

    @SafeParceled(1)
    private int type;

    @SafeParceled(2)
    private int confidence;

    private DetectedActivity() {
    }


    /**
     * Constructs a DetectedActivity.
     *
     * @param activityType the activity that was detected.
     * @param confidence   value from 0 to 100 indicating how likely it is that the user is performing this activity.
     */
    public DetectedActivity(int activityType, int confidence) {
        this.type = activityType;
        this.confidence = confidence;
    }

    @PublicApi(exclude = true)
    public DetectedActivity(int versionCode, int type, int confidence) {
        this.versionCode = versionCode;
        this.type = type;
        this.confidence = confidence;
    }

    /**
     * Returns a value from 0 to 100 indicating the likelihood that the user is performing this
     * activity.
     * <p>
     * The larger the value, the more consistent the data used to perform the classification is
     * with the detected activity.
     * <p>
     * This value will be <= 100. It means that larger values indicate that it's likely that the
     * detected activity is correct, while a value of <= 50 indicates that there may be another
     * activity that is just as or more likely.
     * <p>
     * Multiple activities may have high confidence values. For example, the ON_FOOT may have a
     * confidence of 100 while the RUNNING activity may have a confidence of 95. The sum of the
     * confidences of all detected activities for a classification does not have to be <= 100 since
     * some activities are not mutually exclusive (for example, you can be walking while in a bus)
     * and some activities are hierarchical (ON_FOOT is a generalization of WALKING and RUNNING).
     */
    public int getConfidence() {
        return confidence;
    }

    /**
     * Returns the type of activity that was detected.
     */
    public int getType() {
        return type;
    }

    @PublicApi(exclude = true)
    public int getVersionCode() {
        return versionCode;
    }

    @Override
    public String toString() {
        return "DetectedActivity [type=" + typeToString(getType()) + ", confidence=" + getConfidence() + "]";
    }

    @PublicApi(exclude = true)
    public static String typeToString(int type) {
        switch (type) {
            case 0:
                return "IN_VEHICLE";
            case 1:
                return "ON_BICYCLE";
            case 2:
                return "ON_FOOT";
            case 3:
                return "STILL";
            case 4:
                return "UNKNOWN";
            case 5:
                return "TILTING";
            case 6:
            default:
                return Integer.toString(type);
            case 7:
                return "WALKING";
            case 8:
                return "RUNNING";
        }
    }

    public static final Creator<DetectedActivity> CREATOR = new AutoCreator<DetectedActivity>(DetectedActivity.class);
}
