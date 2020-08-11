/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.measurement.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class EventParcel extends AutoSafeParcelable {
    public static final Creator<EventParcel> CREATOR = new AutoCreator<>(EventParcel.class);
}
