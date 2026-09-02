/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.hms;

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
import com.google.android.gms.maps.internal.ICameraUpdateFactoryDelegate;
import com.google.android.gms.maps.internal.ICreator;
import com.google.android.gms.maps.internal.IMapFragmentDelegate;
import com.google.android.gms.maps.internal.IMapViewDelegate;
import com.google.android.gms.maps.internal.IStreetViewPanoramaFragmentDelegate;
import com.google.android.gms.maps.internal.IStreetViewPanoramaViewDelegate;
import com.google.android.gms.maps.model.internal.IBitmapDescriptorFactoryDelegate;

import org.microg.gms.maps.hms.CameraUpdateFactoryImpl;
import org.microg.gms.maps.hms.MapFragmentImpl;
import org.microg.gms.maps.hms.MapViewImpl;
import org.microg.gms.maps.hms.StreetViewPanoramaFragmentImpl;
import org.microg.gms.maps.hms.StreetViewPanoramaViewImpl;
import org.microg.gms.maps.hms.model.BitmapDescriptorFactoryImpl;

/**
 * HMS (Huawei Map Kit)-based maps renderer entry point.
 *
 * <p>This is the runtime-selected {@link ICreator} implementation backed by Huawei Map Kit. It must
 * not share a package/class name with the Mapbox or VTM renderer implementations so that all
 * renderers can be bundled into a single APK and selected at runtime via the MicroG
 * \"Map renderer\" setting.</p>
 */
@Keep
public class HmsCreator extends ICreator.Stub {
    private static final String TAG = "GmsMapCreator";
    public static volatile int VERSION = Integer.MAX_VALUE;

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
        VERSION = flags;
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
