package org.microg.gms.common.api;

import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class GoogleApiClientImpl implements GoogleApiClient {
    private final Context context;
    private final Looper looper;
    private final AccountInfo accountInfo;
    private final Map<Api, Api.ApiOptions> apis = new HashMap<>();
    private final Map<Api, Api.Connection> apiConnections = new HashMap<>();
    private final Set<ConnectionCallbacks> connectionCallbacks = new HashSet<>();
    private final Set<OnConnectionFailedListener> connectionFailedListeners = new HashSet<>();
    private final ConnectionCallbacks baseConnectionCallbacks = new ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle connectionHint) {
            for (ConnectionCallbacks callback : connectionCallbacks) {
                callback.onConnected(connectionHint);
            }
        }

        @Override
        public void onConnectionSuspended(int cause) {
            for (ConnectionCallbacks callback : connectionCallbacks) {
                callback.onConnectionSuspended(cause);
            }
        }
    };
    private final OnConnectionFailedListener baseConnectionFailedListener = new OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(ConnectionResult result) {
            for (OnConnectionFailedListener listener : connectionFailedListeners) {
                listener.onConnectionFailed(result);
            }
        }
    };
    private final int clientId;

    public GoogleApiClientImpl(Context context, Looper looper, AccountInfo accountInfo,
            Map<Api, Api.ApiOptions> apis,
            Set<ConnectionCallbacks> connectionCallbacks,
            Set<OnConnectionFailedListener> connectionFailedListeners, int clientId) {
        this.context = context;
        this.looper = looper;
        this.accountInfo = accountInfo;
        this.apis.putAll(apis);
        this.connectionCallbacks.addAll(connectionCallbacks);
        this.connectionFailedListeners.addAll(connectionFailedListeners);
        this.clientId = clientId;
        
        for (Api api : apis.keySet()) {
            apiConnections.put(api, api.getBuilder().build(context, looper,
                    apis.get(api), accountInfo, baseConnectionCallbacks,
                    baseConnectionFailedListener));
        }
    }
    
    public Api.Connection getApiConnection(Api api) {
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
    public void connect() {
        for (Api.Connection connection : apiConnections.values()) {
            connection.connect();
        }
    }

    @Override
    public void disconnect() {
        for (Api.Connection connection : apiConnections.values()) {
            connection.disconnect();
        }
    }

    @Override
    public boolean isConnected() {
        for (Api.Connection connection : apiConnections.values()) {
            if (!connection.isConnected()) return false;
        }
        return true;
    }

    @Override
    public boolean isConnecting() {
        return false; // TODO
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
    public void reconnect() {
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
