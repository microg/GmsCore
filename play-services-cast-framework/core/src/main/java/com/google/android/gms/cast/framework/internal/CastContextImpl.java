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
import android.util.Log;

import androidx.mediarouter.media.MediaControlIntent;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;

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
import java.util.HashMap;

public class CastContextImpl extends ICastContext.Stub {
    private static final String TAG = CastContextImpl.class.getSimpleName();

    private SessionManagerImpl sessionManager;
    private DiscoveryManagerImpl discoveryManager;

    private Context context;
    private CastOptions options;
    private IMediaRouter router;
    private Map<String, ISessionProvider> sessionProviders = new HashMap<String, ISessionProvider>();
    public ISessionProvider defaultSessionProvider;

    private MediaRouteSelector mergedSelector;

    public CastContextImpl(IObjectWrapper context, CastOptions options, IMediaRouter router, Map<String, IBinder> sessionProviders) throws RemoteException {
        this.context = (Context) ObjectWrapper.unwrap(context);
        this.options = options;
        this.router = router;
        for (Map.Entry<String, IBinder> entry : sessionProviders.entrySet()) {
            this.sessionProviders.put(entry.getKey(), ISessionProvider.Stub.asInterface(entry.getValue()));
        }

        String receiverApplicationId = options.getReceiverApplicationId();
        String defaultCategory = CastMediaControlIntent.categoryForCast(receiverApplicationId);

        this.defaultSessionProvider = this.sessionProviders.get(defaultCategory);
        if (this.defaultSessionProvider == null) {
            // The provider map can be keyed by the full control category, which carries extra
            // namespace/flag suffixes (e.g. ".../CC1AD845///ALLOW_IPV6"), rather than the bare
            // categoryForCast(appId). Fall back to matching by category prefix.
            for (Map.Entry<String, ISessionProvider> entry : this.sessionProviders.entrySet()) {
                if (entry.getKey() != null && entry.getKey().startsWith(defaultCategory)) {
                    this.defaultSessionProvider = entry.getValue();
                    break;
                }
            }
        }

        // TODO: This should incorporate passed options
        this.mergedSelector = new MediaRouteSelector.Builder()
            .addControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO)
            .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
            .addControlCategory(defaultCategory)
            .build();

        // Observe route selection so that choosing a Cast route actually starts a session.
        // This goes through the app's MediaRouterProxy (IMediaRouter), which runs androidx
        // MediaRouter in the app process; touching MediaRouter directly from the dynamite would
        // fail with a Resources$NotFoundException. On selection the app invokes
        // MediaRouterCallbackImpl.onRouteSelected(), which starts the session.
        try {
            this.router.registerMediaRouterCallbackImpl(this.mergedSelector.asBundle(),
                    new MediaRouterCallbackImpl(this));
            this.router.addCallback(this.mergedSelector.asBundle(),
                    MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to register media router callback: " + e.getMessage());
        }
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
