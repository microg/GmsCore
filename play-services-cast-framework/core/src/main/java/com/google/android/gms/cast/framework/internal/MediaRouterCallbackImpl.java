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
import android.os.RemoteException;
import android.util.Log;

/**
 * Receives route events from the MediaRouter of the client app.
 * <p>
 * Depending on the version of the client library and of androidx.mediarouter, selecting a route is reported through
 * {@link #onRouteSelected}, {@link #onRouteSelectedWithRequestedRoute}, {@link #onRouteConnected} or several of
 * them. {@link SessionManagerImpl} thus has to treat these events as idempotent.
 */
public class MediaRouterCallbackImpl extends IMediaRouterCallback.Stub {
    private static final String TAG = "GmsCastRouterCallback";

    private final CastContextImpl castContext;

    public MediaRouterCallbackImpl(CastContextImpl castContext) {
        this.castContext = castContext;
    }

    @Override
    public void onRouteAdded(String routeId, Bundle extras) {
        castContext.getDiscoveryManager().updateDeviceAvailability();
        castContext.getSessionManagerImpl().onRoutesChanged();
    }

    @Override
    public void onRouteChanged(String routeId, Bundle extras) {
        castContext.getDiscoveryManager().updateDeviceAvailability();
        castContext.getSessionManagerImpl().onRoutesChanged();
        castContext.getSessionManagerImpl().onRouteChanged(routeId, extras);
    }

    @Override
    public void onRouteRemoved(String routeId, Bundle extras) {
        castContext.getDiscoveryManager().updateDeviceAvailability();
    }

    @Override
    public void onRouteSelected(String routeId, Bundle extras) throws RemoteException {
        Log.d(TAG, "onRouteSelected: " + routeId);
        castContext.getSessionManagerImpl().onRouteSelected(routeId, extras);
    }

    @Override
    public void onRouteUnselectedLegacy(String routeId, Bundle extras) {
        Log.d(TAG, "onRouteUnselected: " + routeId);
        castContext.getSessionManagerImpl().onRouteUnselected(routeId, 0);
    }

    @Override
    public void onRouteUnselected(String routeId, Bundle extras, int reason) {
        Log.d(TAG, "onRouteUnselected: " + routeId + " reason=" + reason);
        castContext.getSessionManagerImpl().onRouteUnselected(routeId, reason);
    }

    @Override
    public int getSupportedVersion() {
        return CastDynamiteModuleImpl.MODULE_VERSION;
    }

    @Override
    public void onRouteSelectedWithRequestedRoute(String requestedRouteId, String routeId, Bundle extras) {
        Log.d(TAG, "onRouteSelected: " + routeId + " requested=" + requestedRouteId);
        castContext.getSessionManagerImpl().onRouteSelected(routeId, extras);
    }

    @Override
    public void onRouteConnected(String requestedRouteId, String routeId, Bundle extras) {
        Log.d(TAG, "onRouteConnected: " + routeId + " requested=" + requestedRouteId);
        castContext.getSessionManagerImpl().onRouteSelected(routeId, extras);
    }

    @Override
    public void onRouteDisconnected(String requestedRouteId, String routeId, Bundle extras, int reason) {
        Log.d(TAG, "onRouteDisconnected: " + routeId + " requested=" + requestedRouteId + " reason=" + reason);
        castContext.getSessionManagerImpl().onRouteUnselected(routeId, reason);
    }
}
