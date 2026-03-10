/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
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
import com.google.android.gms.maps.StreetViewPanoramaOptions;
import com.google.android.gms.maps.model.internal.IBitmapDescriptorFactoryDelegate;

import com.huawei.hms.maps.MapsInitializer;
import org.microg.gms.maps.hms.CameraUpdateFactoryImpl;
import org.microg.gms.maps.hms.MapFragmentImpl;
import org.microg.gms.maps.hms.MapViewImpl;
import org.microg.gms.maps.hms.StreetViewPanoramaFragmentImpl;
import org.microg.gms.maps.hms.StreetViewPanoramaViewImpl;
import org.microg.gms.maps.hms.model.BitmapDescriptorFactoryImpl;

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
        BitmapDescriptorFactoryImpl.INSTANCE.initialize(ObjectWrapper.unwrapTyped(resources, Resources.class));
        //ResourcesContainer.set((Resources) ObjectWrapper.unwrap(resources));
        Log.d(TAG, "initV2 " + flags);
    }

    @Override
    public IStreetViewPanoramaViewDelegate newStreetViewPanoramaViewDelegate(IObjectWrapper context, StreetViewPanoramaOptions options) {
        return new StreetViewPanoramaViewImpl(ObjectWrapper.unwrapTyped(context, Context.class));
    }

    @Override
    public IStreetViewPanoramaFragmentDelegate newStreetViewPanoramaFragmentDelegate(IObjectWrapper activity) {
        return new StreetViewPanoramaFragmentImpl(ObjectWrapper.unwrapTyped(activity, Activity.class));
    }

    @Override
    public int getRendererType() throws RemoteException {
        return 2;
    }

    @Override
    public void logInitialization(IObjectWrapper context, int preferredRenderer) throws RemoteException {
        Log.d(TAG, "HMS-based Map initialized (preferred renderer was " + preferredRenderer + ")");
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (super.onTransact(code, data, reply, flags)) return true;
        Log.d(TAG, "onTransact [unknown]: " + code + ", " + data + ", " + flags);
        return false;
    }
}
