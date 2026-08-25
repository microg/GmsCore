/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps;

import android.app.Activity;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.internal.IGoogleMapDelegate;
import com.google.android.gms.maps.internal.IMapViewDelegate;
import com.google.android.gms.maps.internal.IOnMapReadyCallback;
import com.google.android.gms.maps.model.RuntimeRemoteException;

public class MapViewDelegate implements MapLifecycleDelegate {
    private final ViewGroup container;
    private final IMapViewDelegate delegate;
    private View view;

    public MapViewDelegate(ViewGroup container, IMapViewDelegate delegate) {
        this.container = container;
        this.delegate = delegate;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
        throw new UnsupportedOperationException("onCreateView not allowed on MapViewDelegate");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        try {
            Bundle temp = new Bundle();
            MapsBundleHelper.transfer(savedInstanceState, temp);
            delegate.onCreate(temp);
            MapsBundleHelper.transfer(temp, savedInstanceState);
            view = (View) ObjectWrapper.unwrap(delegate.getView());
            container.removeAllViews();
            container.addView(view);
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    @Override
    public void onDestroy() {
        try {
            delegate.onDestroy();
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    @Override
    public void onDestroyView() {
        throw new UnsupportedOperationException("onDestroyView not allowed on MapViewDelegate");
    }

    public void onEnterAmbient(Bundle bundle) {
        try {
            Bundle temp = new Bundle();
            MapsBundleHelper.transfer(bundle, temp);
            delegate.onEnterAmbient(temp);
            MapsBundleHelper.transfer(temp, bundle);
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    public void onExitAmbient() {
        try {
            delegate.onExitAmbient();
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    @Override
    public void onInflate(@NonNull Activity activity, @NonNull Bundle options, @Nullable Bundle onInflate) {
        throw new UnsupportedOperationException("onInflate not allowed on MapViewDelegate");
    }

    @Override
    public void onLowMemory() {
        try {
            delegate.onLowMemory();
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    @Override
    public void onPause() {
        try {
            delegate.onPause();
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    @Override
    public void onResume() {
        try {
            delegate.onResume();
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        try {
            Bundle temp = new Bundle();
            MapsBundleHelper.transfer(outState, temp);
            delegate.onSaveInstanceState(temp);
            MapsBundleHelper.transfer(temp, outState);
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    @Override
    public void onStart() {
        try {
            delegate.onStart();
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    @Override
    public void onStop() {
        try {
            delegate.onStop();
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    @Override
    public void getMapAsync(@NonNull OnMapReadyCallback callback) {
        try {
            delegate.getMapAsync(new IOnMapReadyCallback.Stub() {
                @Override
                public void onMapReady(IGoogleMapDelegate map) throws RemoteException {
                    callback.onMapReady(new GoogleMap(map));
                }
            });
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }
}
