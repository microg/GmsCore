/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.vtm;

import android.app.Activity;
import android.os.Bundle;
import android.os.RemoteException;
import android.widget.TextView;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.maps.internal.IOnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.internal.IStreetViewPanoramaDelegate;
import com.google.android.gms.maps.internal.IStreetViewPanoramaViewDelegate;

public class StreetViewPanoramaViewImpl extends IStreetViewPanoramaViewDelegate.Stub {

    private Activity mActivity;

    public StreetViewPanoramaViewImpl(Activity activity) {
        this.mActivity = activity;
    }

    @Override
    public IStreetViewPanoramaDelegate getStreetViewPanorama() throws RemoteException {
        return null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) throws RemoteException {

    }

    @Override
    public void onResume() throws RemoteException {

    }

    @Override
    public void onPause() throws RemoteException {

    }

    @Override
    public void onDestroy() throws RemoteException {

    }

    @Override
    public void onLowMemory() throws RemoteException {

    }

    @Override
    public void onSaveInstanceState(Bundle outState) throws RemoteException {

    }

    @Override
    public IObjectWrapper getView() throws RemoteException {
        return ObjectWrapper.wrap(new TextView(mActivity));
    }

    @Override
    public void getStreetViewPanoramaAsync(IOnStreetViewPanoramaReadyCallback callback) throws RemoteException {

    }

    @Override
    public void onStart() throws RemoteException {

    }

    @Override
    public void onStop() throws RemoteException {

    }
}
