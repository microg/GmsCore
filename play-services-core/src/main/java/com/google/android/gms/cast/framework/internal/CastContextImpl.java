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
import android.os.RemoteException;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouteSelector;
import android.util.Log;

import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.ICastContext;
import com.google.android.gms.cast.framework.IDiscoveryManager;
import com.google.android.gms.cast.framework.ISessionManager;
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
    private Map map;

    private MediaRouteSelector mergedSelector;

    public CastContextImpl(IObjectWrapper context, CastOptions options, IMediaRouter router, Map map) {
        Log.d(TAG, "Creating new cast context");
        this.context = (Context) ObjectWrapper.unwrap(context);
        this.options = options;
        this.router = router;
        this.map = map;

        // TODO: This should incorporate passed options
        this.mergedSelector = new MediaRouteSelector.Builder()
            .addControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO)
            .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
            .build();
    }

    @Override
    public Bundle getMergedSelectorAsBundle() throws RemoteException {
        return this.mergedSelector.asBundle();
    }

    @Override
    public boolean isApplicationVisible() throws RemoteException {
        Log.d(TAG, "unimplemented Method: isApplicationVisible");
        return true;
    }

    @Override
    public ISessionManager getSessionManagerImpl() throws RemoteException {
        if (this.sessionManager == null) {
            this.sessionManager = new SessionManagerImpl();
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
    public void unknown(String s1, Map m1) throws RemoteException {
        Log.d(TAG, "unimplemented Method: unknown");
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

    @Override
    public IObjectWrapper getWrappedThis() throws RemoteException {
        return ObjectWrapper.wrap(this);
    }
}
