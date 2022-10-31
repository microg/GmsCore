/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import android.content.Intent;

import org.microg.safeparcel.AutoSafeParcelable;

import java.util.List;

/**
 * Represents a sleep classification event including the classification timestamp, the sleep confidence, and the
 * supporting data such as device motion and ambient light level. Classification events are reported at a regular
 * intervals, such as every 10 minutes.
 */
public class SleepClassifyEvent extends AutoSafeParcelable {
    public static final Creator<SleepClassifyEvent> CREATOR = new AutoCreator<>(SleepClassifyEvent.class);

    /**
     * Extracts the {@code SleepClassifyEvent} from an {@code Intent}.
     *
     * @param intent the {@code Intent} to extract from
     * @return a list of {@link SleepClassifyEvent}s if the intent has events, or an empty list if the intent doesn't
     * contain any events.
     */
    public static List<SleepClassifyEvent> extractEvents(Intent intent) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a sleep confidence value between 0 and 100. Higher values indicate that the user is more likely sleeping,
     * while lower values indicate that the user is more likely awake.
     */
    public int getConfidence() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the brightness of the space around the device, based on the device's ambient light sensor readings. Value
     * ranges from 1 to 6, inclusive. Higher values indicate brighter surroundings, while lower values indicate darker
     * surroundings.
     */
    public int getLight() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the amount of device motion, based on the device's accelerometer readings. Value ranges from 1 to 6,
     * inclusive. Higher values indicate more movement of the device.
     */
    public int getMotion() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the UNIX epoch time when the event happened, expressed as the number of milliseconds since 1/1/1970 UTC.
     */
    public long getTimestampMillis() {
        throw new UnsupportedOperationException();
    }

    /**
     * Checks whether the {@code Intent} has any {@code SleepClassifyEvent}.
     *
     * @param intent the {@code Intent} to extract from
     * @return true if the Intent has events
     */
    public static boolean hasEvents(Intent intent) {
        throw new UnsupportedOperationException();
    }
}
