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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.internal.ClientSettings;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class GoogleApiClientImpl extends GoogleApiClient {
    private static final String TAG = "GmsApiClientImpl";

    private final Context context;
    private final Looper looper;
    private final ClientSettings clientSettings;
    private final Map<Api, Api.ApiOptions> apis = new HashMap<Api, Api.ApiOptions>();
    private final Map<Api, Api.Client> apiConnections = new HashMap<Api, Api.Client>();
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

    public GoogleApiClientImpl(Context context, Looper looper, ClientSettings clientSettings,
                               Map<Api<?>, Api.ApiOptions> apis,
                               Set<ConnectionCallbacks> connectionCallbacks,
                               Set<OnConnectionFailedListener> connectionFailedListeners, int clientId) {
        this.context = context;
        this.looper = looper;
        this.clientSettings = clientSettings;
        this.apis.putAll(apis);
        this.connectionCallbacks.addAll(connectionCallbacks);
        this.connectionFailedListeners.addAll(connectionFailedListeners);
        this.clientId = clientId;

        if (this.clientSettings.getSessionId() == null) {
            this.clientSettings.setSessionId(hashCode());
        }

        for (Api api : apis.keySet()) {
            apiConnections.put(api, api.getClientBuilder().buildClient(context, looper, clientSettings, apis.get(api), baseConnectionCallbacks, baseConnectionFailedListener));
        }
    }

    public synchronized void incrementUsageCounter() {
        usageCounter++;
    }

    public synchronized void decrementUsageCounter() {
        usageCounter--;
        if (shouldDisconnect) disconnect();
    }

    @NonNull
    public Looper getLooper() {
        return looper;
    }

    @Override
    public boolean hasConnectedApi(@NonNull Api<?> api) {
        return getApiConnection(api).isConnected();
    }

    public Api.Client getApiConnection(Api api) {
        return apiConnections.get(api);
    }

    @NonNull
    @Override
    public ConnectionResult blockingConnect() {
        return null;
    }

    @NonNull
    @Override
    public ConnectionResult blockingConnect(long timeout, @NonNull TimeUnit unit) {
        return null;
    }

    @NonNull
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
        for (Api.Client connection : apiConnections.values()) {
            if (!connection.isConnected()) {
                connection.connect();
            }
        }
    }

    @Override
    public void connect(int signInMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void disconnect() {
        if (usageCounter > 0) {
            shouldDisconnect = true;
        } else {
            Log.d(TAG, "disconnect()");
            for (Api.Client connection : apiConnections.values()) {
                if (connection.isConnected()) {
                    connection.disconnect();
                }
            }
        }
    }

    @Override
    public void dump(@NonNull String prefix, @Nullable FileDescriptor fd, @NonNull PrintWriter writer, @Nullable String[] args) {
        throw new UnsupportedOperationException();
    }

    @NonNull
    @Override
    public ConnectionResult getConnectionResult(@NonNull Api<?> api) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized boolean isConnected() {
        for (Api.Client connection : apiConnections.values()) {
            if (!connection.isConnected()) return false;
        }
        return true;
    }

    @Override
    public synchronized boolean isConnecting() {
        for (Api.Client connection : apiConnections.values()) {
            if (connection.isConnecting()) return true;
        }
        return false;
    }

    @Override
    public boolean isConnectionCallbacksRegistered(@NonNull ConnectionCallbacks listener) {
        return connectionCallbacks.contains(listener);
    }

    @Override
    public boolean isConnectionFailedListenerRegistered(@NonNull OnConnectionFailedListener listener) {
        return connectionFailedListeners.contains(listener);
    }

    @Override
    public synchronized void reconnect() {
        Log.d(TAG, "reconnect()");
        disconnect();
        connect();
    }

    @Override
    public void registerConnectionCallbacks(@NonNull GoogleApiClient.ConnectionCallbacks listener) {
        connectionCallbacks.add(listener);
    }

    @Override
    public void registerConnectionFailedListener(@NonNull GoogleApiClient.OnConnectionFailedListener listener) {
        connectionFailedListeners.add(listener);
    }

    @Override
    public void stopAutoManage(@NonNull FragmentActivity lifecycleActivity) throws IllegalStateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unregisterConnectionCallbacks(@NonNull GoogleApiClient.ConnectionCallbacks listener) {
        connectionCallbacks.remove(listener);
    }

    @Override
    public void unregisterConnectionFailedListener(@NonNull GoogleApiClient.OnConnectionFailedListener listener) {
        connectionFailedListeners.remove(listener);
    }
}
