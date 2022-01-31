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
import com.google.android.gms.dynamic.ObjectWrapper;

public class MediaRouterCallbackImpl extends IMediaRouterCallback.Stub {
    private static final String TAG = MediaRouterCallbackImpl.class.getSimpleName();

    private final CastContextImpl castContext;

    public MediaRouterCallbackImpl(CastContextImpl castContext) {
        this.castContext = castContext;
    }

    @Override
    public void onRouteAdded(String routeId, Bundle extras) {
        Log.d(TAG, "unimplemented Method: onRouteAdded");
    }
    @Override
    public void onRouteChanged(String routeId, Bundle extras) {
        Log.d(TAG, "unimplemented Method: onRouteChanged");
    }
    @Override
    public void onRouteRemoved(String routeId, Bundle extras) {
        Log.d(TAG, "unimplemented Method: onRouteRemoved");
    }
    @Override
    public void onRouteSelected(String routeId, Bundle extras) throws RemoteException {
        CastDevice castDevice = CastDevice.getFromBundle(extras);

        SessionImpl session = (SessionImpl) ObjectWrapper.unwrap(this.castContext.defaultSessionProvider.getSession(null));
        Bundle routeInfoExtras = this.castContext.getRouter().getRouteInfoExtrasById(routeId);
        if (routeInfoExtras != null) {
            session.start(this.castContext, castDevice, routeId, routeInfoExtras);
        }
    }
    @Override
    public void unknown(String routeId, Bundle extras) {
        Log.d(TAG, "unimplemented Method: unknown");
    }
    @Override
    public void onRouteUnselected(String routeId, Bundle extras, int reason) {
        Log.d(TAG, "unimplemented Method: onRouteUnselected");
    }
}
