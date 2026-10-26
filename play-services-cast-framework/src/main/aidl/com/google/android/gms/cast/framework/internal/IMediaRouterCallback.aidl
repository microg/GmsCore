package com.google.android.gms.cast.framework.internal;

import android.os.Bundle;

interface IMediaRouterCallback {
    void onRouteAdded(String routeId, in Bundle extras) = 0;
    void onRouteChanged(String routeId, in Bundle extras) = 1;
    void onRouteRemoved(String routeId, in Bundle extras) = 2;
    void onRouteSelected(String routeId, in Bundle extras) = 3;
    void unknown(String routeId, in Bundle extras) = 4;
    void onRouteUnselected(String routeId, in Bundle extras, int reason) = 5;
    int getSupportedVersion() = 6;
    void onRouteSelectedWithRequestedRoute(String requestedRouteId, String selectedRouteId, in Bundle extras) = 7;
    void onRouteConnected(String requestedRouteId, String connectedRouteId, in Bundle extras) = 8;
    void onRouteDisconnected(String requestedRouteId, String disconnectedRouteId, in Bundle extras, int reason) = 9;
}
