/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.location.internal;

import android.app.PendingIntent;

import com.google.android.gms.location.ILocationCallback;
import com.google.android.gms.location.ILocationListener;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class LocationRequestUpdateData extends AutoSafeParcelable {

    public static final int REQUEST_UPDATES = 1;
    public static final int REMOVE_UPDATES = 2;

    @SafeParceled(1000)
    private int versionCode;

    @SafeParceled(1)
    public int opCode;

    @SafeParceled(2)
    public LocationRequestInternal request;

    @SafeParceled(3)
    public ILocationListener listener;

    @SafeParceled(4)
    public PendingIntent pendingIntent;

    @SafeParceled(5)
    public ILocationCallback callback;

    @SafeParceled(6)
    public IFusedLocationProviderCallback fusedLocationProviderCallback;

    @Override
    public String toString() {
        return "LocationRequestUpdateData{" +
                "opCode=" + opCode +
                ", request=" + request +
                ", listener=" + (listener != null ? listener.asBinder() : null) +
                ", pendingIntent=" + pendingIntent +
                ", callback=" + (callback != null ? callback.asBinder() : null) +
                ", fusedLocationProviderCallback=" + (fusedLocationProviderCallback != null ? fusedLocationProviderCallback.asBinder() : null) +
                '}';
    }

    public static final Creator<LocationRequestUpdateData> CREATOR = new AutoCreator<LocationRequestUpdateData>(LocationRequestUpdateData.class);
}
