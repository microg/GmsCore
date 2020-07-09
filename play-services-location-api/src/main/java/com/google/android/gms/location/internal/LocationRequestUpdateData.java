/*
 * Copyright (C) 2013-2017 microG Project Team
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

package com.google.android.gms.location.internal;

import android.app.PendingIntent;

import com.google.android.gms.location.ILocationCallback;
import com.google.android.gms.location.ILocationListener;
import com.google.android.gms.location.internal.IFusedLocationProviderCallback;

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
