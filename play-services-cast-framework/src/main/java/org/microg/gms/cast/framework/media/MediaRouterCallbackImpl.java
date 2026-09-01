/*
 * Copyright (C) 2013-2026 microG Project Team
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

package org.microg.gms.cast.framework.media;

import android.os.RemoteException;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.mediarouter.media.MediaRouter;
import org.microg.gms.cast.framework.ISessionManager;

public class MediaRouterCallbackImpl extends MediaRouter.Callback {
    private static final String TAG = "MediaRouterCallbackImpl";
    private final ISessionManager sessionManager;

    public MediaRouterCallbackImpl(ISessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void onRouteSelected(@NonNull MediaRouter router, @NonNull MediaRouter.RouteInfo route, int reason) {
        Log.d(TAG, "onRouteSelected: " + route.getName() + " (reason: " + reason + ")");
        try {
            if (sessionManager != null && route.getExtras() != null) {
                sessionManager.onRouteSelected(route.getExtras());
            }
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to forward onRouteSelected", e);
        }
    }

    @Override
    public void onRouteUnselected(@NonNull MediaRouter router, @NonNull MediaRouter.RouteInfo route, int reason) {
        Log.d(TAG, "onRouteUnselected: " + route.getName() + " (reason: " + reason + ")");
        try {
            if (sessionManager != null) {
                sessionManager.onRouteUnselected();
            }
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to forward onRouteUnselected", e);
        }
    }

    @Override
    public void onRouteAdded(@NonNull MediaRouter router, @NonNull MediaRouter.RouteInfo route) {
        checkAvailability(router);
    }

    @Override
    public void onRouteRemoved(@NonNull MediaRouter router, @NonNull MediaRouter.RouteInfo route) {
        checkAvailability(router);
    }

    @Override
    public void onRouteChanged(@NonNull MediaRouter router, @NonNull MediaRouter.RouteInfo route) {
        checkAvailability(router);
    }

    private void checkAvailability(MediaRouter router) {
        boolean hasCastDevice = false;
        for (MediaRouter.RouteInfo route : router.getRoutes()) {
            if (!route.isDefault() && route.getExtras() != null && route.getExtras().containsKey("com.google.android.gms.cast.EXTRA_CAST_DEVICE")) {
                hasCastDevice = true;
                break;
            }
        }
        try {
            if (sessionManager != null) {
                sessionManager.onDeviceAvailabilityChanged(hasCastDevice);
            }
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to notify device availability", e);
        }
    }
}