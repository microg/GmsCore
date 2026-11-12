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

import android.annotation.SuppressLint;
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
import com.google.android.gms.cast.framework.media.IMediaNotificationService;
import com.google.android.gms.cast.framework.media.internal.IFetchBitmapTask;
import com.google.android.gms.cast.framework.media.internal.IFetchBitmapTaskProgressPublisher;
import com.google.android.gms.dynamic.IObjectWrapper;

import java.util.Map;

/**
 * Entry point of the {@code com.google.android.gms.cast.framework.dynamite} module, loaded into the process of the
 * client app by {@code DynamiteModule}. As there are no classes shared with the client, all interaction is through
 * binder interfaces and thus transaction ids must match those used by the client library.
 */
public class CastDynamiteModuleImpl extends ICastDynamiteModule.Stub {
    private static final String TAG = "GmsCastDynamiteModule";

    /**
     * Version reported to the client library. The client uses it to decide which variant of a call to use, the first
     * newer variants appear with 220400000 (see {@link IMediaRouterCallback}). We stay below so that the client sticks
     * to the calls that exist in all versions of the client library.
     */
    static final int MODULE_VERSION = 12451000;

    // CastContext is a singleton in the client that lives as long as the process, so there is at most one of these
    @SuppressLint("StaticFieldLeak")
    private static volatile CastContextImpl castContext;

    @Override
    public ICastContext newCastContextImpl(IObjectWrapper context, CastOptions options, IMediaRouter router, Map sessionProviders) throws RemoteException {
        CastContextImpl impl = new CastContextImpl(context, options, router, sessionProviders);
        castContext = impl;
        return impl;
    }

    @Override
    public ISession newSessionImpl(String category, String sessionId, ISessionProxy proxy) throws RemoteException {
        return new SessionImpl(castContext, category, sessionId, proxy);
    }

    @Override
    public ICastSession newCastSessionImpl(CastOptions options, IObjectWrapper session, ICastConnectionController controller) throws RemoteException {
        return new CastSessionImpl(options, session, controller);
    }

    @Override
    public IMediaNotificationService newMediaNotificationServiceImpl(IObjectWrapper service, IObjectWrapper castContext, IObjectWrapper resources, CastMediaOptions options) throws RemoteException {
        // Not used by current client libraries, which build the media notification themselves
        Log.d(TAG, "unimplemented Method: newMediaNotificationServiceImpl");
        return null;
    }

    @Override
    public IReconnectionService newReconnectionServiceImpl(IObjectWrapper service, IObjectWrapper sessionManager, IObjectWrapper discoveryManager) throws RemoteException {
        return new ReconnectionServiceImpl();
    }

    @Override
    public IFetchBitmapTask newFetchBitmapTaskImpl(IObjectWrapper asyncTask, IFetchBitmapTaskProgressPublisher progressPublisher, int preferredWidth, int preferredHeight, boolean allowLargerThanPreferred, long maxBytes, int maxRedirects, int progressIntervalMillis, int timeoutMillis) throws RemoteException {
        return new FetchBitmapTaskImpl(progressPublisher, preferredWidth, preferredHeight, allowLargerThanPreferred, maxBytes, progressIntervalMillis, timeoutMillis);
    }

    @Override
    public IFetchBitmapTask newFetchBitmapTaskImplWithContext(IObjectWrapper context, IObjectWrapper asyncTask, IFetchBitmapTaskProgressPublisher progressPublisher, int preferredWidth, int preferredHeight, boolean allowLargerThanPreferred, long maxBytes, int maxRedirects, int progressIntervalMillis, int timeoutMillis) throws RemoteException {
        return new FetchBitmapTaskImpl(progressPublisher, preferredWidth, preferredHeight, allowLargerThanPreferred, maxBytes, progressIntervalMillis, timeoutMillis);
    }

    @Override
    public int getSupportedVersion() throws RemoteException {
        return MODULE_VERSION;
    }
}
