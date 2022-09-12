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

import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;

import androidx.fragment.app.FragmentActivity;

import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class GoogleApiClientImpl implements GoogleApiClient {
    private static final String TAG = "GmsApiClientImpl";

    private final Context context;
    private final Looper looper;
    private final ApiClientSettings clientSettings;
    private final Map<Api, Api.ApiOptions> apis = new HashMap<Api, Api.ApiOptions>();
    private final Map<Api, ApiClient> apiConnections = new HashMap<Api, ApiClient>();
    private final Set<ConnectionCallbacks> connectionCallbacks = new HashSet<ConnectionCallbacks>();
    private final Set<OnConnectionFailedListener> connectionFailedListeners = new HashSet<OnConnectionFailedListener>();
    private final int clientId;
    private final ConnectionCallbacks baseConnectionCallbacks = new ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle connectionHint) {
            Log.d(TAG, "ConnectionCallbacks : onConnected()");
            for (ConnectionCallbacks callback : connectionCallbacks) {
                callback.onConnected(connectionHint);
            }
        }

        @Override
        public void onConnectionSuspended(int cause) {
            Log.d(TAG, "ConnectionCallbacks : onConnectionSuspended()");
            for (ConnectionCallbacks callback : connectionCallbacks) {
                callback.onConnectionSuspended(cause);
            }
        }
    };
    private final OnConnectionFailedListener baseConnectionFailedListener = new
            OnConnectionFailedListener() {
                @Override
                public void onConnectionFailed(ConnectionResult result) {
                    Log.d(TAG, "OnConnectionFailedListener : onConnectionFailed()");
                    for (OnConnectionFailedListener listener : connectionFailedListeners) {
                        listener.onConnectionFailed(result);
                    }
                }
            };
    private int usageCounter = 0;
    private boolean shouldDisconnect = false;

    public GoogleApiClientImpl(Context context, Looper looper, ApiClientSettings clientSettings,
                               Map<Api, Api.ApiOptions> apis,
                               Set<ConnectionCallbacks> connectionCallbacks,
                               Set<OnConnectionFailedListener> connectionFailedListeners, int clientId) {
        this.context = context;
        this.looper = looper;
        this.clientSettings = clientSettings;
        this.apis.putAll(apis);
        this.connectionCallbacks.addAll(connectionCallbacks);
        this.connectionFailedListeners.addAll(connectionFailedListeners);
        this.clientId = clientId;

        for (Api api : apis.keySet()) {
            apiConnections.put(api, api.getBuilder().build(apis.get(api), context, looper, clientSettings, baseConnectionCallbacks, baseConnectionFailedListener));
        }
    }

    public synchronized void incrementUsageCounter() {
        usageCounter++;
    }

    public synchronized void decrementUsageCounter() {
        usageCounter--;
        if (shouldDisconnect) disconnect();
    }

    public Looper getLooper() {
        return looper;
    }

    public ApiClient getApiConnection(Api api) {
        return apiConnections.get(api);
    }

    @Override
    public ConnectionResult blockingConnect() {
        return null;
    }

    @Override
    public ConnectionResult blockingConnect(long timeout, TimeUnit unit) {
        return null;
    }

    @Override
    public PendingResult<Status> clearDefaultAccountAndReconnect() {
        return null;
    }

    @Override
    public synchronized void connect() {
        Log.d(TAG, "connect()");
        if (isConnected() || isConnecting()) {
            if (shouldDisconnect) {
                shouldDisconnect = false;
                return;
            }
            Log.d(TAG, "Already connected/connecting, nothing to do");
            return;
        }
        for (ApiClient connection : apiConnections.values()) {
            if (!connection.isConnected()) {
                connection.connect();
            }
        }
    }

    @Override
    public synchronized void disconnect() {
        if (usageCounter > 0) {
            shouldDisconnect = true;
        } else {
            Log.d(TAG, "disconnect()");
            for (ApiClient connection : apiConnections.values()) {
                if (connection.isConnected()) {
                    connection.disconnect();
                }
            }
        }
    }

    @Override
    public synchronized boolean isConnected() {
        for (ApiClient connection : apiConnections.values()) {
            if (!connection.isConnected()) return false;
        }
        return true;
    }

    @Override
    public synchronized boolean isConnecting() {
        for (ApiClient connection : apiConnections.values()) {
            if (connection.isConnecting()) return true;
        }
        return false;
    }

    @Override
    public boolean isConnectionCallbacksRegistered(ConnectionCallbacks listener) {
        return connectionCallbacks.contains(listener);
    }

    @Override
    public boolean isConnectionFailedListenerRegistered(
            OnConnectionFailedListener listener) {
        return connectionFailedListeners.contains(listener);
    }

    @Override
    public synchronized void reconnect() {
        Log.d(TAG, "reconnect()");
        disconnect();
        connect();
    }

    @Override
    public void registerConnectionCallbacks(ConnectionCallbacks listener) {
        connectionCallbacks.add(listener);
    }

    @Override
    public void registerConnectionFailedListener(OnConnectionFailedListener listener) {
        connectionFailedListeners.add(listener);
    }

    @Override
    public void stopAutoManager(FragmentActivity lifecycleActivity) throws IllegalStateException {

    }

    @Override
    public void unregisterConnectionCallbacks(ConnectionCallbacks listener) {
        connectionCallbacks.remove(listener);
    }

    @Override
    public void unregisterConnectionFailedListener(OnConnectionFailedListener listener) {
        connectionFailedListeners.remove(listener);
    }
}
