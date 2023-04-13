package com.google.android.gms.cast.framework.internal;

import android.os.Bundle;

interface IMediaRouterCallback {
    void onRouteAdded(String routeId, in Bundle extras) = 0;
    void onRouteChanged(String routeId, in Bundle extras) = 1;
    void onRouteRemoved(String routeId, in Bundle extras) = 2;
    void onRouteSelected(String routeId, in Bundle extras) = 3;
    void unknown(String routeId, in Bundle extras) = 4;
    void onRouteUnselected(String routeId, in Bundle extras, int reason) = 5;
}
