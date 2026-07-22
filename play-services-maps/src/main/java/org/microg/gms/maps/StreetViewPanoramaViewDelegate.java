/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
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
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.internal.*;
import com.google.android.gms.maps.model.RuntimeRemoteException;

public class StreetViewPanoramaViewDelegate implements StreetViewLifecycleDelegate {
    private final ViewGroup container;
    private final IStreetViewPanoramaViewDelegate delegate;
    private View view;

    public StreetViewPanoramaViewDelegate(ViewGroup container, IStreetViewPanoramaViewDelegate delegate) {
        this.container = container;
        this.delegate = delegate;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
        throw new UnsupportedOperationException("onCreateView not allowed on StreetViewPanoramaViewDelegate");
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
        throw new UnsupportedOperationException("onDestroyView not allowed on StreetViewPanoramaViewDelegate");
    }

    @Override
    public void onInflate(@NonNull Activity activity, @NonNull Bundle options, @Nullable Bundle onInflate) {
        throw new UnsupportedOperationException("onInflate not allowed on StreetViewPanoramaViewDelegate");
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
    public void getStreetViewPanoramaAsync(@NonNull OnStreetViewPanoramaReadyCallback callback) {
        try {
            delegate.getStreetViewPanoramaAsync(new IOnStreetViewPanoramaReadyCallback.Stub() {
                @Override
                public void onStreetViewPanoramaReady(IStreetViewPanoramaDelegate streetViewPanorama) throws RemoteException {
                    callback.onStreetViewPanoramaReady(new StreetViewPanorama(streetViewPanorama));
                }
            });
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }
}
