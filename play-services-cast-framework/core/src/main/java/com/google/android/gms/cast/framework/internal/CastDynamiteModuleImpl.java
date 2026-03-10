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
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.ICastConnectionController;
import com.google.android.gms.cast.framework.ICastContext;
import com.google.android.gms.cast.framework.ICastSession;
import com.google.android.gms.cast.framework.IReconnectionService;
import com.google.android.gms.cast.framework.ISession;
import com.google.android.gms.cast.framework.ISessionProxy;
import com.google.android.gms.cast.framework.media.CastMediaOptions;
import com.google.android.gms.cast.framework.internal.CastContextImpl;
import com.google.android.gms.cast.framework.internal.CastSessionImpl;
import com.google.android.gms.cast.framework.internal.MediaRouterCallbackImpl;
import com.google.android.gms.cast.framework.internal.SessionImpl;
import com.google.android.gms.cast.framework.media.IMediaNotificationService;
import com.google.android.gms.cast.framework.media.internal.IFetchBitmapTask;
import com.google.android.gms.cast.framework.media.internal.IFetchBitmapTaskProgressPublisher;
import com.google.android.gms.dynamic.IObjectWrapper;

import java.util.Map;

public class CastDynamiteModuleImpl extends ICastDynamiteModule.Stub {
    private static final String TAG = CastDynamiteModuleImpl.class.getSimpleName();

    @Override
    public ICastContext newCastContextImpl(IObjectWrapper context, CastOptions options, IMediaRouter router, Map sessionProviders) throws RemoteException {
        return new CastContextImpl(context, options, router, sessionProviders);
    }

    @Override
    public ISession newSessionImpl(String category, String sessionId, ISessionProxy proxy) throws RemoteException {
        return new SessionImpl(category, sessionId, proxy);
    }

    @Override
    public ICastSession newCastSessionImpl(CastOptions options, IObjectWrapper session, ICastConnectionController controller) throws RemoteException {
        return new CastSessionImpl(options, session, controller);
    }

    @Override
    public IMediaNotificationService newMediaNotificationServiceImpl(IObjectWrapper service, IObjectWrapper castContext, IObjectWrapper resources, CastMediaOptions options) throws RemoteException {
        Log.d(TAG, "unimplemented Method: newMediaNotificationServiceImpl");
        return null;
    }

    @Override
    public IReconnectionService newReconnectionServiceImpl(IObjectWrapper service, IObjectWrapper sessionManager, IObjectWrapper discoveryManager) throws RemoteException {
        Log.d(TAG, "unimplemented Method: newReconnectionServiceImpl");
        return null;
    }

    @Override
    public IFetchBitmapTask newFetchBitmapTaskImpl(IObjectWrapper asyncTask, IFetchBitmapTaskProgressPublisher progressPublisher, int i1, int i2, boolean b1, long l1, int i3, int i4, int i5) throws RemoteException {
        Log.d(TAG, "unimplemented Method: newFetchBitmapTaskImpl");
        return null;
    }
}
