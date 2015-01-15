package org.microg.gms.common;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.api.GoogleApiClient;

public final class ForwardConnectionFailedListener
        implements GoogleApiClient.OnConnectionFailedListener {
    private final GooglePlayServicesClient.OnConnectionFailedListener listener;

    public ForwardConnectionFailedListener(
            GooglePlayServicesClient.OnConnectionFailedListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ForwardConnectionFailedListener &&
                listener.equals(((ForwardConnectionFailedListener) o).listener);
    }

    @Override
    public int hashCode() {
        return listener.hashCode();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        listener.onConnectionFailed(result);
    }
}
