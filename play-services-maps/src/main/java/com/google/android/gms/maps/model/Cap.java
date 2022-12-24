/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.maps.model;

import android.os.IBinder;

import org.microg.safeparcel.AutoSafeParcelable;

public class Cap extends AutoSafeParcelable {
    @Field(2)
    private int type;
    @Field(3)
    private IBinder bitmap;
    private BitmapDescriptor bitmapDescriptor;
    @Field(4)
    private float bitmapRefWidth;
    public static final Creator<Cap> CREATOR = new AutoCreator<>(Cap.class);
}
