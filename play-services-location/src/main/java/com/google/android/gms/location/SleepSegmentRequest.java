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
 * A request object that specifies what data to receive from the Sleep API. Defaults to {@code SEGMENT_AND_CLASSIFY_EVENTS}.
 */
public class SleepSegmentRequest extends AutoSafeParcelable {
    /**
     * Requests both the {@code SleepSegmentEvent} and the {@code SleepClassifyEvent}.
     */
    public static final int SEGMENT_AND_CLASSIFY_EVENTS = 0;
    /**
     * Requests {@code SleepSegmentEvent} only.
     */
    public static final int SEGMENT_EVENTS_ONLY = 1;
    /**
     * Requests {@code SleepClassifyEvent} only.
     */
    public static final int CLASSIFY_EVENTS_ONLY = 2;

    public static final Creator<SleepSegmentRequest> CREATOR = new AutoCreator<>(SleepSegmentRequest.class);

    private int requestedDataType;

    private SleepSegmentRequest() {

    }

    /**
     * Constructs a {@link SleepSegmentRequest} indicating what type of data is being requested.
     *
     * @param requestedDataType The type of data to receive pending intents for; valid values are
     *                          {@link #SEGMENT_AND_CLASSIFY_EVENTS}, {@link #SEGMENT_EVENTS_ONLY}, and {@link #CLASSIFY_EVENTS_ONLY}.
     */
    public SleepSegmentRequest(int requestedDataType) {
        this.requestedDataType = requestedDataType;
    }

    public boolean equals(Object o) {
        return o instanceof SleepSegmentRequest && ((SleepSegmentRequest) o).requestedDataType == requestedDataType;
    }

    /**
     * Creates a default request that registers for both {@code SleepSegmentEvent} and {@code SleepClassifyEvent} data.
     */
    public static SleepSegmentRequest getDefaultSleepSegmentRequest() {
        return new SleepSegmentRequest(SEGMENT_AND_CLASSIFY_EVENTS);
    }

    /**
     * Returns the requested data type, which is one of {@link #SEGMENT_AND_CLASSIFY_EVENTS}, {@link #SEGMENT_EVENTS_ONLY},
     * or {@link #CLASSIFY_EVENTS_ONLY}.
     */
    public int getRequestedDataType() {
        return requestedDataType;
    }

    @Override
    public int hashCode() {
        return requestedDataType;
    }
}
