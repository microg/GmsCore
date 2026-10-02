/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.cast.framework.internal;

import android.os.Bundle;
import android.util.Log;

import org.microg.gms.common.Constants;

/**
 * Receives media route events from the client library's MediaRouter for routes matching the merged selector.
 */
public class MediaRouterCallbackImpl extends IMediaRouterCallback.Stub {
    private static final String TAG = MediaRouterCallbackImpl.class.getSimpleName();

    private final CastContextImpl castContext;

    public MediaRouterCallbackImpl(CastContextImpl castContext) {
        this.castContext = castContext;
    }

    private SessionManagerImpl getSessionManager() {
        return castContext.getSessionManagerImpl();
    }

    void updateCastState() {
        getSessionManager().updateCastState();
    }

    @Override
    public void onRouteAdded(String routeId, Bundle extras) {
        Log.d(TAG, "onRouteAdded: " + routeId);
        updateCastState();
        getSessionManager().onRouteAdded(routeId);
    }

    @Override
    public void onRouteChanged(String routeId, Bundle extras) {
        Log.d(TAG, "onRouteChanged: " + routeId);
        getSessionManager().onRouteChanged(routeId, extras);
        updateCastState();
    }

    @Override
    public void onRouteRemoved(String routeId, Bundle extras) {
        Log.d(TAG, "onRouteRemoved: " + routeId);
        updateCastState();
    }

    @Override
    public void onRouteSelected(String routeId, Bundle extras) {
        Log.d(TAG, "onRouteSelected: " + routeId);
        getSessionManager().onRouteSelected(routeId, extras);
    }

    @Override
    public void unknown(String routeId, Bundle extras) {
        Log.d(TAG, "unimplemented Method: unknown");
    }

    @Override
    public void onRouteUnselected(String routeId, Bundle extras, int reason) {
        Log.d(TAG, "onRouteUnselected: " + routeId + " reason=" + reason);
        getSessionManager().onRouteUnselected(routeId, reason);
        updateCastState();
    }

    @Override
    public int getSupportedVersion() {
        return Constants.GMS_VERSION_CODE;
    }

    @Override
    public void onRouteSelectedWithRequestedRoute(String requestedRouteId, String selectedRouteId, Bundle extras) {
        Log.d(TAG, "onRouteSelected: " + selectedRouteId + " (requested " + requestedRouteId + ")");
        getSessionManager().onRouteSelected(selectedRouteId, extras);
    }

    @Override
    public void onRouteConnected(String requestedRouteId, String connectedRouteId, Bundle extras) {
        // Route connections are used for media transfer (output switcher), they don't start a session on their own.
        Log.d(TAG, "onRouteConnected: " + connectedRouteId + " (requested " + requestedRouteId + ")");
    }

    @Override
    public void onRouteDisconnected(String requestedRouteId, String disconnectedRouteId, Bundle extras, int reason) {
        Log.d(TAG, "onRouteDisconnected: " + disconnectedRouteId + " reason=" + reason);
        getSessionManager().onRouteUnselected(disconnectedRouteId, reason);
        updateCastState();
    }
}
