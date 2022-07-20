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

package com.google.android.gms.maps.internal;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Parcel;
import android.os.RemoteException;
import androidx.annotation.Keep;
import android.util.Log;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.model.internal.IBitmapDescriptorFactoryDelegate;

import org.microg.gms.maps.mapbox.CameraUpdateFactoryImpl;
import org.microg.gms.maps.mapbox.MapFragmentImpl;
import org.microg.gms.maps.mapbox.MapViewImpl;
import org.microg.gms.maps.mapbox.model.BitmapDescriptorFactoryImpl;

@Keep
public class CreatorImpl extends ICreator.Stub {
    private static final String TAG = "GmsMapCreator";

    @Override
    public void init(IObjectWrapper resources) {
        initV2(resources, 0);
    }

    @Override
    public IMapFragmentDelegate newMapFragmentDelegate(IObjectWrapper activity) {
        return new MapFragmentImpl(ObjectWrapper.unwrapTyped(activity, Activity.class));
    }

    @Override
    public IMapViewDelegate newMapViewDelegate(IObjectWrapper context, GoogleMapOptions options) {
        return new MapViewImpl(ObjectWrapper.unwrapTyped(context, Context.class), options);
    }

    @Override
    public ICameraUpdateFactoryDelegate newCameraUpdateFactoryDelegate() {
        return new CameraUpdateFactoryImpl();
    }

    @Override
    public IBitmapDescriptorFactoryDelegate newBitmapDescriptorFactoryDelegate() {
        return BitmapDescriptorFactoryImpl.INSTANCE;
    }

    @Override
    public void initV2(IObjectWrapper resources, int flags) {
        BitmapDescriptorFactoryImpl.INSTANCE.initialize(ObjectWrapper.unwrapTyped(resources, Resources.class), null);
        //ResourcesContainer.set((Resources) ObjectWrapper.unwrap(resources));
        Log.d(TAG, "initV2 " + flags);
    }

    @Override
    public int getRendererType() throws RemoteException {
        return 2;
    }

    @Override
    public void logInitialization(IObjectWrapper context, int preferredRenderer) throws RemoteException {
        Log.d(TAG, "Mapbox-based Map initialized (preferred renderer was " + preferredRenderer + ")");
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (super.onTransact(code, data, reply, flags)) return true;
        Log.d(TAG, "onTransact [unknown]: " + code + ", " + data + ", " + flags);
        return false;
    }
}
