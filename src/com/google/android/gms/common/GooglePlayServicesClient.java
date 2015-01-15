package com.google.android.gms.common;

import android.os.Bundle;

@Deprecated
public interface GooglePlayServicesClient {
    void connect();

    void disconnect();

    boolean isConnected();

    boolean isConnecting();

    void registerConnectionCallbacks(ConnectionCallbacks listener);

    boolean isConnectionCallbacksRegistered(ConnectionCallbacks listener);

    void unregisterConnectionCallbacks(ConnectionCallbacks listener);

    void registerConnectionFailedListener(OnConnectionFailedListener listener);

    boolean isConnectionFailedListenerRegistered(OnConnectionFailedListener listener);

    void unregisterConnectionFailedListener(OnConnectionFailedListener listener);

    @Deprecated
    public interface OnConnectionFailedListener {

        void onConnectionFailed(ConnectionResult result);
    }

    @Deprecated
    public interface ConnectionCallbacks {

        void onConnected(Bundle connectionHint);

        void onDisconnected();
    }
}
