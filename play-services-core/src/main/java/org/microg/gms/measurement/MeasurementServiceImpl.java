/*
 * Copyright (C) 2018 microG Project Team
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

package org.microg.gms.measurement;

import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.measurement.internal.AppMetadata;
import com.google.android.gms.measurement.internal.ConditionalUserPropertyParcel;
import com.google.android.gms.measurement.internal.EventParcel;
import com.google.android.gms.measurement.internal.IMeasurementService;

public class MeasurementServiceImpl extends IMeasurementService.Stub {
    private static final String TAG = "GmsMeasureSvcImpl";

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (super.onTransact(code, data, reply, flags)) return true;
        Log.d(TAG, "onTransact [unknown]: " + code + ", " + data + ", " + flags);
        return false;
    }

    @Override
    public void f1(EventParcel p0, AppMetadata p1) throws RemoteException {
        Log.d(TAG, "f1: " + p1.packageName);
    }

    @Override
    public void f4(AppMetadata p0) throws RemoteException {
        Log.d(TAG, "f4: " + p0.packageName);
    }

    @Override
    public void f10(long p0, String p1, String p2, String p3) throws RemoteException {
        Log.d(TAG, "f10: " + p1);
    }

    @Override
    public String f11(AppMetadata p0) throws RemoteException {
        Log.d(TAG, "f11: " + p0.packageName);
        return null;
    }

    @Override
    public void f12(ConditionalUserPropertyParcel p0, AppMetadata p1) throws RemoteException {
        Log.d(TAG, "f12: " + p1.packageName);
    }
}
