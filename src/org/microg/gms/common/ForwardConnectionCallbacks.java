package org.microg.gms.common;

import android.os.Bundle;

import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.api.GoogleApiClient;

public final class ForwardConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {
    private final GooglePlayServicesClient.ConnectionCallbacks callbacks;

    public ForwardConnectionCallbacks(GooglePlayServicesClient.ConnectionCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ForwardConnectionCallbacks &&
                callbacks.equals(((ForwardConnectionCallbacks) o).callbacks);
    }

    @Override
    public int hashCode() {
        return callbacks.hashCode();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        callbacks.onConnected(connectionHint);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        callbacks.onDisconnected();
    }
}
