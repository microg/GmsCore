package com.google.android.gms.location;

import android.content.Context;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.api.GoogleApiClient;
import org.microg.gms.common.ForwardConnectionCallbacks;
import org.microg.gms.common.ForwardConnectionFailedListener;

@Deprecated
public class LocationClient implements GooglePlayServicesClient {
    private GoogleApiClient googleApiClient;
    
    public LocationClient(Context context, ConnectionCallbacks callbacks) {
        googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new ForwardConnectionCallbacks(callbacks))
                .build();
    }

    @Override
    public void connect() {
        googleApiClient.connect();
    }

    @Override
    public void disconnect() {
        googleApiClient.disconnect();
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
