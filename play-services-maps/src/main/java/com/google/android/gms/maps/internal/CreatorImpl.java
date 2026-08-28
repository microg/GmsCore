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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Keep;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.StreetViewPanoramaOptions;
import com.google.android.gms.maps.model.internal.IBitmapDescriptorFactoryDelegate;

/**
 * Single entry point for the Maps API.
 *
 * <p>Google Maps clients (e.g. Google Photos) look this class up by its legacy fully-qualified name
 * {@code com.google.android.gms.maps.internal.CreatorImpl} inside MicroG's classloader, so it must
 * keep this exact name. All renderer engines (Mapbox/MapLibre and VTM) ship in the same APK, and a
 * MicroG setting selects which one renders maps. This class reads that setting and delegates every
 * {@link ICreator} call to the selected renderer's {@link ICreator} implementation, so switching
 * the renderer works at runtime without a reinstall.</p>
 */
@Keep
public class CreatorImpl extends ICreator.Stub {
    private static final String TAG = "GmsMapCreator";
    // "Map renderer" dropdown in MicroG's Location settings. Stored as a string in the default
    // shared preferences; the settings ListPreference writes it directly. "stadia" and "hms" are
    // only offered by the settings UI once the matching API key has been configured.
    private static final String PREF_MAP_ENGINE = "pref_map_engine";
    private static final String MAP_ENGINE_MAPBOX = "mapbox";
    private static final String MAP_ENGINE_VTM = "vtm";
    private static final String MAP_ENGINE_STADIA = "stadia";
    private static final String MAP_ENGINE_HMS = "hms";
    private static final String RENDERER_MAPBOX = "org.microg.gms.maps.mapbox.MapboxCreator";
    private static final String RENDERER_VTM = "org.microg.gms.maps.vtm.VtmCreator";
    private static final String RENDERER_HMS = "org.microg.gms.maps.hms.HmsCreator";

    private final ICreator delegate;

    private static String getSelectedRendererClassName() {
        String selection = MAP_ENGINE_MAPBOX;
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                    CreatorImpl.classToContext());
            selection = prefs.getString(PREF_MAP_ENGINE, MAP_ENGINE_MAPBOX);
        } catch (Throwable t) {
            Log.w(TAG, "Could not read map renderer preference, defaulting to mapbox", t);
        }
        if (MAP_ENGINE_VTM.equalsIgnoreCase(selection)) {
            return RENDERER_VTM;
        }
        if (MAP_ENGINE_HMS.equalsIgnoreCase(selection)) {
            return RENDERER_HMS;
        }
        // "stadia" and "mapbox" both use the Mapbox/MapLibre engine; the Stadia tile source is
        // chosen by the renderer itself based on the configured API key. Anything unknown falls
        // back to mapbox.
        return RENDERER_MAPBOX;
    }

    private static ICreator instantiate(String className) {
        try {
            return ICreator.Stub.asInterface((IBinder) Class.forName(className).newInstance());
        } catch (Throwable t) {
            Log.w(TAG, "Unable to instantiate maps renderer " + className + ", falling back to mapbox", t);
            try {
                return ICreator.Stub.asInterface((IBinder) Class.forName(RENDERER_MAPBOX).newInstance());
            } catch (Throwable t2) {
                throw new IllegalStateException("Unable to instantiate maps renderer " + RENDERER_MAPBOX, t2);
            }
        }
    }

    /**
     * Returns an application context without a strong reference into app state. Falls back to null
     * if unavailable; the preference lookup tolerates that.
     */
    private static Context classToContext() {
        try {
            return (Context) Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication").invoke(null);
        } catch (Throwable t) {
            return null;
        }
    }

    public CreatorImpl() {
        delegate = instantiate(getSelectedRendererClassName());
    }

    @Override
    public void init(IObjectWrapper resources) throws RemoteException {
        delegate.init(resources);
    }

    @Override
    public IMapFragmentDelegate newMapFragmentDelegate(IObjectWrapper activity) throws RemoteException {
        return delegate.newMapFragmentDelegate(activity);
    }

    @Override
    public IMapViewDelegate newMapViewDelegate(IObjectWrapper context, GoogleMapOptions options) throws RemoteException {
        return delegate.newMapViewDelegate(context, options);
    }

    @Override
    public ICameraUpdateFactoryDelegate newCameraUpdateFactoryDelegate() throws RemoteException {
        return delegate.newCameraUpdateFactoryDelegate();
    }

    @Override
    public IBitmapDescriptorFactoryDelegate newBitmapDescriptorFactoryDelegate() throws RemoteException {
        return delegate.newBitmapDescriptorFactoryDelegate();
    }

    @Override
    public void initV2(IObjectWrapper resources, int flags) throws RemoteException {
        delegate.initV2(resources, flags);
    }

    @Override
    public IStreetViewPanoramaViewDelegate newStreetViewPanoramaViewDelegate(IObjectWrapper context, StreetViewPanoramaOptions options) throws RemoteException {
        return delegate.newStreetViewPanoramaViewDelegate(context, options);
    }

    @Override
    public IStreetViewPanoramaFragmentDelegate newStreetViewPanoramaFragmentDelegate(IObjectWrapper activity) throws RemoteException {
        return delegate.newStreetViewPanoramaFragmentDelegate(activity);
    }

    @Override
    public int getRendererType() throws RemoteException {
        return delegate.getRendererType();
    }

    @Override
    public void logInitialization(IObjectWrapper context, int preferredRenderer) throws RemoteException {
        delegate.logInitialization(context, preferredRenderer);
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (super.onTransact(code, data, reply, flags)) return true;
        Log.d(TAG, "onTransact [unknown]: " + code + ", " + data + ", " + flags);
        return false;
    }
}