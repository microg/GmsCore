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
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import androidx.mediarouter.media.MediaRouter;

import com.google.android.gms.cast.framework.IDiscoveryManager;
import com.google.android.gms.cast.framework.IDiscoveryManagerListener;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;


/**
 * Requests route discovery from the MediaRouter of the client app while the app is visible and keeps track of whether
 * any matching route is available.
 */
public class DiscoveryManagerImpl extends IDiscoveryManager.Stub {
    private static final String TAG = "GmsCastDiscoveryManager";

    private final CastContextImpl castContext;
    private final ListenerRegistry<IDiscoveryManagerListener> listeners = new ListenerRegistry<>(TAG, IDiscoveryManagerListener::getWrappedThis);

    private Bundle addedSelector;
    private boolean explicitDiscovery = false;
    private boolean deviceAvailable = false;

    public DiscoveryManagerImpl(CastContextImpl castContext) {
        this.castContext = castContext;
    }

    /**
     * (Re-)adds our router callback. Discovery is only requested while it is useful, the callback itself stays
     * registered so that we still learn about route (un)selection while the app is in background.
     */
    private void updateRouterCallback() {
        Bundle selector = castContext.getRegisteredSelectorBundle();
        if (selector == null) return;
        boolean discover = castContext.isAppVisible() || explicitDiscovery;
        try {
            IMediaRouter router = castContext.getRouter();
            if (addedSelector != null) {
                // Flags of an existing callback can only be extended on older MediaRouter versions, so remove it first
                router.removeCallback(addedSelector);
            }
            router.addCallback(selector, discover ? MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY : 0);
            addedSelector = selector;
            Log.d(TAG, "Router callback added, discovery=" + discover);
        } catch (Exception e) {
            Log.w(TAG, "Failed to update router callback", e);
        }
        updateDeviceAvailability();
    }

    void onSelectorChanged() {
        // CastContextImpl cleared all callbacks before registering for the new selector
        addedSelector = null;
        updateRouterCallback();
    }

    void onAppVisibilityChanged() {
        updateRouterCallback();
    }

    /**
     * To be invoked whenever the set of routes known to the MediaRouter of the client changed.
     */
    void updateDeviceAvailability() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            // Clients may create their CastContext on a background thread, but their MediaRouter can only be queried
            // on the main thread.
            castContext.getMainHandler().post(this::updateDeviceAvailability);
            return;
        }
        Bundle selector = castContext.getRegisteredSelectorBundle();
        if (selector == null) return;
        boolean available;
        try {
            available = castContext.getRouter().isRouteAvailable(selector, MediaRouter.AVAILABILITY_FLAG_IGNORE_DEFAULT_ROUTE);
        } catch (Exception e) {
            Log.w(TAG, "Failed to query route availability", e);
            return;
        }
        if (available == deviceAvailable) return;
        deviceAvailable = available;
        Log.d(TAG, "Device availability changed: " + available);

        this.listeners.forEach("onDeviceAvailabilityChanged", listener -> listener.onDeviceAvailabilityChanged(available));
        castContext.getSessionManagerImpl().onDeviceAvailabilityChanged(available);
    }

    @Override
    public void startDiscovery() {
        explicitDiscovery = true;
        updateRouterCallback();
    }

    @Override
    public void stopDiscovery() {
        explicitDiscovery = false;
        updateRouterCallback();
    }

    @Override
    public void addDiscoveryManagerListener(IDiscoveryManagerListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public void removeDiscoveryManagerListener(IDiscoveryManagerListener listener) {
        this.listeners.remove(listener);
    }

    @Override
    public IObjectWrapper getWrappedThis() throws RemoteException {
        return ObjectWrapper.wrap(this);
    }
}
