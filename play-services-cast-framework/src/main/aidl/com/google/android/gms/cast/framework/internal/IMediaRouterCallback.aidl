package com.google.android.gms.cast.framework.internal;

import android.os.Bundle;

interface IMediaRouterCallback {
    void onRouteAdded(String routeId, in Bundle extras) = 0;
    void onRouteChanged(String routeId, in Bundle extras) = 1;
    void onRouteRemoved(String routeId, in Bundle extras) = 2;
    void onRouteSelected(String routeId, in Bundle extras) = 3;
    void onRouteUnselectedLegacy(String routeId, in Bundle extras) = 4;
    void onRouteUnselected(String routeId, in Bundle extras, int reason) = 5;
    /**
     * The client picks the callback variants based on this:
     * >= 220400000: onRouteSelectedWithRequestedRoute() instead of onRouteSelected()
     * >= 251600000: onRouteConnected() / onRouteDisconnected()
     * For lower values, the client still invokes onRouteSelectedWithRequestedRoute() for MediaRouter's onRouteConnected
     * and onRouteUnselected() for MediaRouter's onRouteDisconnected.
     */
    int getSupportedVersion() = 6;
    void onRouteSelectedWithRequestedRoute(String requestedRouteId, String routeId, in Bundle extras) = 7;
    void onRouteConnected(String requestedRouteId, String routeId, in Bundle extras) = 8;
    void onRouteDisconnected(String requestedRouteId, String routeId, in Bundle extras, int reason) = 9;
}
