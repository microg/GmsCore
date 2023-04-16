package com.google.android.gms.cast.framework.internal;

import android.os.Bundle;

import com.google.android.gms.cast.framework.internal.IMediaRouterCallback;

interface IMediaRouter {
    void registerMediaRouterCallbackImpl(in Bundle selector, IMediaRouterCallback callback) = 0;
    void addCallback(in Bundle selector, int flags) = 1;
    void removeCallback(in Bundle selector) = 2;
    boolean isRouteAvailable(in Bundle selector, int flags) = 3;
    void selectRouteById(String routeId) = 4;
    void selectDefaultRoute() = 5;
    boolean isDefaultRouteSelected() = 6; // Maybe?
    Bundle getRouteInfoExtrasById(String routeId) = 7;
    String getSelectedRouteId() = 8; // Maybe?
    int getSupportedVersion() = 9;
    void clearCallbacks() = 10;
}
