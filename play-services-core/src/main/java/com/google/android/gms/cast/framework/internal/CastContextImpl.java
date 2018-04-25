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

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;

import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.IAppVisibilityListener;
import com.google.android.gms.cast.framework.ICastContext;
import com.google.android.gms.cast.framework.IDiscoveryManager;
import com.google.android.gms.cast.framework.ISessionManager;
import com.google.android.gms.cast.framework.ISessionProvider;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;

import java.util.Map;

public class CastContextImpl extends ICastContext.Stub {
    private static final String TAG = CastContextImpl.class.getSimpleName();

    private SessionManagerImpl sessionManager;
    private DiscoveryManagerImpl discoveryManager;

    private Context context;
    private CastOptions options;
    private IMediaRouter router;
    private Map<String, IBinder> sessionProviders;
    public ISessionProvider defaultSessionProvider;

    private MediaRouteSelector mergedSelector;

    public CastContextImpl(IObjectWrapper context, CastOptions options, IMediaRouter router, Map<String, IBinder> sessionProviders) throws RemoteException {
        this.context = (Context) ObjectWrapper.unwrap(context);
        this.options = options;
        this.router = router;
        this.sessionProviders = sessionProviders;

        String receiverApplicationId = options.getReceiverApplicationId();
        String defaultCategory = CastMediaControlIntent.categoryForCast(receiverApplicationId);

        this.defaultSessionProvider = ISessionProvider.Stub.asInterface(this.sessionProviders.get(defaultCategory));

        // TODO: This should incorporate passed options
        this.mergedSelector = new MediaRouteSelector.Builder()
            .addControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO)
            .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
            .build();

        // TODO: Find a home for this once the rest of the implementation
        // becomes more clear. Uncomment this to enable discovery of devices.
        // Note that the scan currently isn't ever disabled as part of the
        // lifecycle, so we don't want to ship with this.
        /*
        Bundle selectorBundle = this.mergedSelector.asBundle();

        router.clearCallbacks();
        router.registerMediaRouterCallbackImpl(selectorBundle, new MediaRouterCallbackImpl(this));
        router.addCallback(selectorBundle, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
        */
    }

    @Override
    public Bundle getMergedSelectorAsBundle() throws RemoteException {
        return this.mergedSelector.asBundle();
    }

    @Override
    public void addVisibilityChangeListener(IAppVisibilityListener listener) {
        Log.d(TAG, "unimplemented Method: addVisibilityChangeListener");
    }

    @Override
    public void removeVisibilityChangeListener(IAppVisibilityListener listener) {
        Log.d(TAG, "unimplemented Method: removeVisibilityChangeListener");
    }

    @Override
    public boolean isApplicationVisible() throws RemoteException {
        Log.d(TAG, "unimplemented Method: isApplicationVisible");
        return true;
    }

    @Override
    public SessionManagerImpl getSessionManagerImpl() {
        if (this.sessionManager == null) {
            this.sessionManager = new SessionManagerImpl(this);
        }
        return this.sessionManager;
    }

    @Override
    public IDiscoveryManager getDiscoveryManagerImpl() throws RemoteException {
        if (this.discoveryManager == null) {
            this.discoveryManager = new DiscoveryManagerImpl(this);
        }
        return this.discoveryManager;
    }

    @Override
    public void destroy() throws RemoteException {
        Log.d(TAG, "unimplemented Method: destroy");
    }

    @Override
    public void onActivityResumed(IObjectWrapper activity) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onActivityResumed");

    }

    @Override
    public void onActivityPaused(IObjectWrapper activity) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onActivityPaused");
    }

    @Override
    public void setReceiverApplicationId(String receiverApplicationId, Map sessionProvidersByCategory) throws RemoteException {
        Log.d(TAG, "unimplemented Method: setReceiverApplicationId");
    }

    public Context getContext() {
        return this.context;
    }

    public IMediaRouter getRouter() {
        return this.router;
    }

    public MediaRouteSelector getMergedSelector() {
        return this.mergedSelector;
    }

    public CastOptions getOptions() {
        return this.options;
    }

    @Override
    public IObjectWrapper getWrappedThis() throws RemoteException {
        return ObjectWrapper.wrap(this);
    }
}
