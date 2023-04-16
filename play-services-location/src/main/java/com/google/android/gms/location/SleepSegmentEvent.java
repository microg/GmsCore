/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import org.microg.safeparcel.AutoSafeParcelable;

/**
 * Represents the result of segmenting sleep after the user is awake.
 */
public class SleepSegmentEvent extends AutoSafeParcelable {
    /**
     * Successfully detected sleep segment in the past day.
     */
    public static final int STATUS_SUCCESSFUL = 0;
    /**
     * Sleep segment was detected, but there was some missing data near the detected sleep segment. This could happen
     * for a variety of reasons, including the following: the user turned off their device, the user delayed logging
     * into their device after a system reboot or system upgrade, or an event occurred that paused the detection.
     */
    public static final int STATUS_MISSING_DATA = 1;
    /**
     * Sleep segment is not detected in the past day, or there isn't enough confidence that the user slept during the
     * past day. This could happen for a variety of reasons, including the following: too much missing data, the user
     * sleeps with the light, the user interacts with their device often, or the user's device doesn't support the
     * sensors needed for sleep detection.
     */
    public static final int STATUS_NOT_DETECTED = 2;

    public static final Creator<SleepSegmentEvent> CREATOR = new AutoCreator<>(SleepSegmentEvent.class);
}
