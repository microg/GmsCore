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

package org.microg.gms.common.api;

import android.util.Log;

import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.api.GoogleApiClient;

import org.microg.gms.common.ForwardConnectionCallbacks;
import org.microg.gms.common.ForwardConnectionFailedListener;

public class AbstractPlayServicesClient implements GooglePlayServicesClient {
    private static final String TAG = "GmsPlayServicesClient";

    protected final GoogleApiClient googleApiClient;

    public AbstractPlayServicesClient(GoogleApiClient googleApiClient) {
        this.googleApiClient = googleApiClient;
    }

    public void assertConnected() {
        if (!isConnected()) throw new IllegalStateException("Not connected!");
    }

    @Override
    public void connect() {
        Log.d(TAG, "connect()");
        googleApiClient.connect();
    }

    @Override
    public void disconnect() {
        Log.d(TAG, "disconnect()");
        //TODO googleApiClient.disconnect();
    }

    @Override
    public boolean isConnected() {
        return googleApiClient.isConnected();
    }

    @Override
    public boolean isConnecting() {
        return googleApiClient.isConnecting();
    }

    @Override
    public void registerConnectionCallbacks(final ConnectionCallbacks listener) {
        googleApiClient.registerConnectionCallbacks(new ForwardConnectionCallbacks(listener));
    }

    @Override
    public boolean isConnectionCallbacksRegistered(ConnectionCallbacks listener) {
        return googleApiClient
                .isConnectionCallbacksRegistered(new ForwardConnectionCallbacks(listener));
    }

    @Override
    public void unregisterConnectionCallbacks(
            ConnectionCallbacks listener) {
        googleApiClient.unregisterConnectionCallbacks(new ForwardConnectionCallbacks(listener));
    }

    @Override
    public void registerConnectionFailedListener(
            OnConnectionFailedListener listener) {
        googleApiClient.registerConnectionFailedListener(
                new ForwardConnectionFailedListener(listener));
    }

    @Override
    public boolean isConnectionFailedListenerRegistered(
            OnConnectionFailedListener listener) {
        return googleApiClient.isConnectionFailedListenerRegistered(
                new ForwardConnectionFailedListener(listener));
    }

    @Override
    public void unregisterConnectionFailedListener(
            OnConnectionFailedListener listener) {
        googleApiClient.unregisterConnectionFailedListener(
                new ForwardConnectionFailedListener(listener));
    }
}
