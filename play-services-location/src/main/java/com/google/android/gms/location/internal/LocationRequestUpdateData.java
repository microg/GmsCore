/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.location.internal;

import android.app.PendingIntent;

import com.google.android.gms.location.ILocationCallback;
import com.google.android.gms.location.ILocationListener;

import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

@Hide
public class LocationRequestUpdateData extends AutoSafeParcelable {

    public static final int REQUEST_UPDATES = 1;
    public static final int REMOVE_UPDATES = 2;

    @Field(1000)
    private int versionCode;

    @Field(1)
    public int opCode;

    @Field(2)
    public LocationRequestInternal request;

    @Field(3)
    public ILocationListener listener;

    @Field(4)
    public PendingIntent pendingIntent;

    @Field(5)
    public ILocationCallback callback;

    @Field(6)
    public IFusedLocationProviderCallback fusedLocationProviderCallback;

    @Field(8)
    public String listenerId;

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
