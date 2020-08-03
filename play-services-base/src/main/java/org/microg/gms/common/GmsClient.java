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

package org.microg.gms.common;

import android.accounts.Account;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.internal.ConnectionInfo;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;
import com.google.android.gms.common.internal.IGmsServiceBroker;

import org.microg.gms.common.api.ApiClient;
import org.microg.gms.common.api.ConnectionCallbacks;
import org.microg.gms.common.api.OnConnectionFailedListener;

public abstract class GmsClient<I extends IInterface> implements ApiClient {
    private static final String TAG = "GmsClient";

    private final Context context;
    protected final ConnectionCallbacks callbacks;
    protected final OnConnectionFailedListener connectionFailedListener;
    protected ConnectionState state = ConnectionState.NOT_CONNECTED;
    private ServiceConnection serviceConnection;
    private I serviceInterface;
    private String actionString;

    protected int serviceId = -1;
    protected Account account = null;
    protected Bundle extras = new Bundle();

    public GmsClient(Context context, ConnectionCallbacks callbacks, OnConnectionFailedListener connectionFailedListener, String actionString) {
        this.context = context;
        this.callbacks = callbacks;
        this.connectionFailedListener = connectionFailedListener;
        this.actionString = actionString;
    }

    protected void onConnectedToBroker(IGmsServiceBroker broker, GmsCallbacks callbacks) throws RemoteException {
        if (serviceId == -1) {
            throw new IllegalStateException("Service ID not set in constructor and onConnectedToBroker not implemented");
        }
        GetServiceRequest request = new GetServiceRequest(serviceId);
        request.extras = new Bundle();
        request.packageName = context.getPackageName();
        request.account = account;
        request.extras = extras;
        broker.getService(callbacks, request);
    }

    protected abstract I interfaceFromBinder(IBinder binder);

    @Override
    public synchronized void connect() {
        Log.d(TAG, "connect()");
        if (state == ConnectionState.CONNECTED || state == ConnectionState.CONNECTING) {
            Log.d(TAG, "Already connected/connecting - nothing to do");
        }
        state = ConnectionState.CONNECTING;
        if (serviceConnection != null) {
            MultiConnectionKeeper.getInstance(context).unbind(actionString, serviceConnection);
        }
        serviceConnection = new GmsServiceConnection();
        if (!MultiConnectionKeeper.getInstance(context).bind(actionString, serviceConnection)) {
            state = ConnectionState.ERROR;
            handleConnectionFailed();
        }
    }

    public void handleConnectionFailed() {
        connectionFailedListener.onConnectionFailed(new ConnectionResult(ConnectionResult.API_UNAVAILABLE, null));
    }

    @Override
    public synchronized void disconnect() {
        Log.d(TAG, "disconnect()");
        if (state == ConnectionState.DISCONNECTING) return;
        if (state == ConnectionState.CONNECTING) {
            state = ConnectionState.DISCONNECTING;
            return;
        }
        serviceInterface = null;
        if (serviceConnection != null) {
            MultiConnectionKeeper.getInstance(context).unbind(actionString, serviceConnection);
            serviceConnection = null;
        }
        state = ConnectionState.NOT_CONNECTED;
    }

    @Override
    public synchronized boolean isConnected() {
        return state == ConnectionState.CONNECTED || state == ConnectionState.PSEUDO_CONNECTED;
    }

    @Override
    public synchronized boolean isConnecting() {
        return state == ConnectionState.CONNECTING;
    }

    public synchronized boolean hasError() {
        return state == ConnectionState.ERROR;
    }

    public Context getContext() {
        return context;
    }

    public synchronized I getServiceInterface() {
        if (isConnecting()) {
            // TODO: wait for connection to be established and return afterwards.
            throw new IllegalStateException("Waiting for connection");
        } else if (!isConnected()) {
            throw new IllegalStateException("interface only available once connected!");
        }
        return serviceInterface;
    }

    protected enum ConnectionState {
        NOT_CONNECTED, CONNECTING, CONNECTED, DISCONNECTING, ERROR, PSEUDO_CONNECTED
    }

    private class GmsServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            try {
                Log.d(TAG, "ServiceConnection : onServiceConnected(" + componentName + ")");
                onConnectedToBroker(IGmsServiceBroker.Stub.asInterface(iBinder), new GmsCallbacks());
            } catch (RemoteException e) {
                disconnect();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            synchronized (GmsClient.this) {
                state = ConnectionState.NOT_CONNECTED;
            }
        }
    }

    public class GmsCallbacks extends IGmsCallbacks.Stub {

        @Override
        public void onPostInitComplete(int statusCode, IBinder binder, Bundle params)
                throws RemoteException {
            synchronized (GmsClient.this) {
                if (state == ConnectionState.DISCONNECTING) {
                    state = ConnectionState.CONNECTED;
                    disconnect();
                    return;
                }
                state = ConnectionState.CONNECTED;
                serviceInterface = interfaceFromBinder(binder);
            }
            Log.d(TAG, "GmsCallbacks : onPostInitComplete(" + serviceInterface + ")");
            callbacks.onConnected(params);
        }

        @Override
        public void onAccountValidationComplete(int statusCode, Bundle params) throws RemoteException {
            Log.d(TAG, "GmsCallbacks : onAccountValidationComplete");
        }

        @Override
        public void onPostInitCompleteWithConnectionInfo(int statusCode, IBinder binder, ConnectionInfo info) throws RemoteException {
            onPostInitComplete(statusCode, binder, info == null ? null : info.params);
        }
    }

}
