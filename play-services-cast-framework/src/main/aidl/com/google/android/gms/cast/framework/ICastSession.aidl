package com.google.android.gms.cast.framework;

import android.os.Bundle;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.common.ConnectionResult;

interface ICastSession {
    void onConnected(in Bundle routeInfoExtra) = 0;
    void onConnectionSuspended(int reason) = 1;
    void onConnectionFailed(in ConnectionResult connectionResult) = 2;
    void onApplicationConnectionSuccess(in ApplicationMetadata applicationMetadata, String applicationStatus, String sessionId, boolean wasLaunched) = 3;
    void onApplicationConnectionFailure(int statusCode) = 4;
    void disconnectFromDevice(boolean stopCasting, int reason) = 5;
}
