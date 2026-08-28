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

package org.microg.gms.maps.mapbox;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Keep;

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

import org.microg.gms.maps.mapbox.CameraUpdateFactoryImpl;
import org.microg.gms.maps.mapbox.model.BitmapDescriptorFactoryImpl;

/**
 * Mapbox / MapLibre-based maps renderer entry point.
 *
 * <p>This is the runtime-selected {@link ICreator} implementation backed by the MapLibre renderer.
 * It must not share a package/class name with the VTM or HMS renderer implementations so that all
 * renderers can be bundled into a single APK and selected at runtime via the MicroG
 * "Map renderer" setting.</p>
 */
@Keep
public class MapboxCreator extends ICreator.Stub {
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
        Log.d(TAG, "Mapbox-based Map initialized (preferred renderer was " + preferredRenderer + ")");
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (super.onTransact(code, data, reply, flags)) return true;
        Log.d(TAG, "onTransact [unknown]: " + code + ", " + data + ", " + flags);
        return false;
    }
}