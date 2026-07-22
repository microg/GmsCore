/*
 * Copyright (C) 2019 microG Project Team
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

package org.microg.gms.maps.vtm;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.internal.IGoogleMapDelegate;
import com.google.android.gms.maps.internal.IMapFragmentDelegate;
import com.google.android.gms.maps.internal.IOnMapReadyCallback;

public class MapFragmentImpl extends IMapFragmentDelegate.Stub {
    private static final String TAG = "GmsMapFragImpl";

    private GoogleMapImpl map;
    private GoogleMapOptions options;
    private Activity activity;

    public MapFragmentImpl(Activity activity) {
        this.activity = activity;
    }

    private GoogleMapImpl myMap() {
        if (map == null) {
            Log.d(TAG, "GoogleMap instance created");
            LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            map = GoogleMapImpl.create(inflater.getContext(), options);
        }
        return map;
    }

    @Override
    public IGoogleMapDelegate getMap() throws RemoteException {
        Log.d(TAG, "getMap");
        return myMap();
    }

    @Override
    public void onInflate(IObjectWrapper activity, GoogleMapOptions options,
                          Bundle savedInstanceState) throws RemoteException {
        if (options != null) this.options = options;
        Log.d(TAG, "onInflate");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) throws RemoteException {
        Log.d(TAG, "onCreate");
        //myMap().onCreate(savedInstanceState);
        // TOOD: Certainly does not belong here and this way
        Bundle mapState = savedInstanceState.getBundle("map_state");
        if (mapState != null) {
            mapState.setClassLoader(GoogleMapOptions.class.getClassLoader());
            GoogleMapOptions options = mapState.getParcelable("MapOptions");
            if (options != null) this.options = options;
        }
    }

    @Override
    public IObjectWrapper onCreateView(IObjectWrapper layoutInflater, IObjectWrapper container,
                                       Bundle savedInstanceState) throws RemoteException {
        Log.d(TAG, "onCreateView");
        if (map == null) {
            LayoutInflater inflater = (LayoutInflater) ObjectWrapper.unwrap(layoutInflater);
            map = GoogleMapImpl.create(inflater.getContext(), options);
            //map.onCreate(savedInstanceState);
        } else {
            View view = map.getView();
            if (view.getParent() instanceof ViewGroup) {
                ((ViewGroup) view.getParent()).removeView(view);
            }
        }
        return ObjectWrapper.wrap(myMap().getView());
    }

    @Override
    public void onResume() throws RemoteException {
        Log.d(TAG, "onResume");
        myMap().onResume();
    }

    @Override
    public void onPause() throws RemoteException {
        Log.d(TAG, "onPause");
        myMap().onPause();
    }

    @Override
    public void onDestroyView() throws RemoteException {
        Log.d(TAG, "onDestroyView");
    }

    @Override
    public void onDestroy() throws RemoteException {
        Log.d(TAG, "onDestroy");
        myMap().onDestroy();
    }

    @Override
    public void onLowMemory() throws RemoteException {
        Log.d(TAG, "onLowMemory");
    }

    @Override
    public void onEnterAmbient(Bundle bundle) throws RemoteException {
        map.onEnterAmbient(bundle);
    }

    @Override
    public void onExitAmbient() throws RemoteException {
        map.onExitAmbient();
    }

    @Override
    public void onStart() throws RemoteException {
        map.onStart();
    }

    @Override
    public void onStop() throws RemoteException {
        map.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) throws RemoteException {
        Log.d(TAG, "onSaveInstanceState: " + outState);
        //myMap().onSaveInstanceState(outState);
    }

    @Override
    public boolean isReady() throws RemoteException {
        Log.d(TAG, "isReady");
        return map != null;
    }

    @Override
    public void getMapAsync(final IOnMapReadyCallback callback) throws RemoteException {
        new Handler(activity.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    callback.onMapReady(myMap());
                } catch (RemoteException e) {
                    Log.w(TAG, e);
                }
            }
        });
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (super.onTransact(code, data, reply, flags)) return true;
        Log.d(TAG, "onTransact [unknown]: " + code + ", " + data + ", " + flags);
        return false;
    }
}
