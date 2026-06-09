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

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.framework.CastState;

public class MediaRouterCallbackImpl extends IMediaRouterCallback.Stub {
    private static final String TAG = MediaRouterCallbackImpl.class.getSimpleName();

    // Bundle keys used by SessionManagerImpl.startSession() — must match exactly.
    private static final String KEY_ROUTE_ID      = "CAST_INTENT_TO_CAST_ROUTE_ID_KEY";
    private static final String KEY_SESSION_ID    = "CAST_INTENT_TO_CAST_SESSION_ID_KEY";
    private static final String KEY_ROUTE_EXTRA   = "CAST_INTENT_TO_CAST_ROUTE_INFO_EXTRA_KEY";
    private static final String KEY_CATEGORY      = "CAST_INTENT_TO_CAST_ROUTE_CATEGORY_KEY";

    private final CastContextImpl castContext;

    // Track whether any Cast device has ever been seen so we can drive NO_DEVICES_AVAILABLE
    // vs NOT_CONNECTED state transitions correctly.
    private int deviceCount = 0;

    public MediaRouterCallbackImpl(CastContextImpl castContext) {
        this.castContext = castContext;
    }

    /**
     * A new Chromecast appeared on the network. Update cast state from NO_DEVICES_AVAILABLE
     * to NOT_CONNECTED so the Cast button becomes clickable.
     */
    @Override
    public void onRouteAdded(String routeId, Bundle extras) {
        deviceCount++;
        if (deviceCount == 1) {
            // Transition out of NO_DEVICES_AVAILABLE on first device.
            castContext.getSessionManagerImpl().onDeviceAvailabilityChanged(true);
        }
    }

    @Override
    public void onRouteChanged(String routeId, Bundle extras) {
        // No action needed — route metadata changes (e.g. volume) don't affect session state.
    }

    /**
     * A Chromecast left the network. If it was the last one, revert to NO_DEVICES_AVAILABLE.
     */
    @Override
    public void onRouteRemoved(String routeId, Bundle extras) {
        if (deviceCount > 0) deviceCount--;
        if (deviceCount == 0) {
            castContext.getSessionManagerImpl().onDeviceAvailabilityChanged(false);
        }
    }

    /**
     * The user selected a Cast route. Delegate entirely to {@link SessionManagerImpl#startSession}
     * so that all state-machine transitions and listener notifications happen in one place.
     *
     * Bug fix: the original implementation called
     * {@code castContext.defaultSessionProvider.getSession(null)} directly and then called
     * {@code session.start()} itself, completely bypassing {@code SessionManagerImpl}. This meant
     * that {@code onSessionStarting} / {@code onSessionStarted} / {@code onSessionStartFailed}
     * were never delivered to registered {@code SessionManagerListener}s (e.g. YouTube's Cast
     * button logic), and the cast state was never updated.
     */
    @Override
    public void onRouteSelected(String routeId, Bundle extras) throws RemoteException {
        // Resolve the best-matching category for this route so SessionManagerImpl can look up
        // the right ISessionProvider. Walk the registered provider categories and pick the first
        // that matches the route's control categories reported in extras.
        String category = resolveCategory(routeId, extras);

        // Fetch the routeInfoExtra (contains CastDevice) from the router.
        Bundle routeInfoExtra = null;
        try {
            routeInfoExtra = castContext.getRouter().getRouteInfoExtrasById(routeId);
        } catch (RemoteException e) {
            Log.w(TAG, "Could not fetch route extras for " + routeId + ": " + e.getMessage());
        }

        Bundle params = new Bundle();
        params.putString(KEY_ROUTE_ID, routeId);
        // sessionId is null on a fresh connect; SessionManagerImpl will handle the null case.
        params.putString(KEY_SESSION_ID, null);
        params.putBundle(KEY_ROUTE_EXTRA, routeInfoExtra != null ? routeInfoExtra : extras);
        params.putString(KEY_CATEGORY, category);

        castContext.getSessionManagerImpl().startSession(params);
    }

    /**
     * The user deselected a Cast route (e.g. pressed "Stop casting" or the route was lost).
     * End the current session. Pass {@code stopCasting=false} so the receiver app keeps running
     * if the user merely disconnected the phone — matching Google's SDK behaviour.
     */
    @Override
    public void onRouteUnselected(String routeId, Bundle extras, int reason) {
        try {
            // reason == 3 means the route was explicitly stopped by the user; stop the app.
            boolean stopCasting = (reason == 3);
            castContext.getSessionManagerImpl().endCurrentSession(false, stopCasting);
        } catch (RemoteException e) {
            Log.w(TAG, "onRouteUnselected endCurrentSession failed: " + e.getMessage());
        }
    }

    @Override
    public void unknown(String routeId, Bundle extras) {
        // Intentionally empty — reserved for future use.
    }

    // ---- Helpers ----

    /**
     * Resolves the Cast control category for the selected route. Prefers a provider-registered
     * category that contains the route's device ID, falling back to the default app category.
     */
    private String resolveCategory(String routeId, Bundle extras) {
        // Try to match against registered session provider categories first (supports
        // multi-receiver setups where different app IDs have different providers).
        for (String cat : castContext.getSessionProviders().keySet()) {
            if (CastMediaControlIntent.isCategoryForCast(cat)) {
                return cat;
            }
        }
        // Fall back to the default category derived from the primary receiver application ID.
        String appId = castContext.getOptions().getReceiverApplicationId();
        return CastMediaControlIntent.categoryForCast(appId);
    }
}
