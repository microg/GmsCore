package com.google.android.gms.cast.framework;

import android.os.Bundle;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.common.api.Status;

interface ICastSession {
    void onConnected(in Bundle routeInfoExtra) = 0;
    void onConnectionSuspended(int reason) = 1;
    void onConnectionFailed(in Status status) = 2;
    void onApplicationConnectionSuccess(in ApplicationMetadata applicationMetadata, String applicationStatus, String sessionId, boolean wasLaunched) = 3;
    void onApplicationConnectionFailure(int statusCode) = 4;
    void disconnectFromDevice(boolean boolean1, int int1) = 5;
}
