/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.measurement.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class EventParcel extends AutoSafeParcelable {
    @Field(2)
    public String name;
    @Field(3)
    public EventParams params;
    @Field(4)
    public String origin;
    @Field(5)
    public long timestamp;

    public static final Creator<EventParcel> CREATOR = new AutoCreator<>(EventParcel.class);
}
