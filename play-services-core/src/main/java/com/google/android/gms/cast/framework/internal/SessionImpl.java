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

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.framework.ISession;
import com.google.android.gms.cast.framework.ISessionProxy;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;

public class SessionImpl extends ISession.Stub {
    private static final String TAG = SessionImpl.class.getSimpleName();

    private final String category;
    private final String sessionId;
    private final ISessionProxy proxy;

    private CastSessionImpl castSession;

    private CastContextImpl castContext;
    private CastDevice castDevice;
    private Bundle routeInfoExtra;

    private boolean mIsConnecting = false;
    private boolean mIsConnected = false;
    private String routeId = null;

    public SessionImpl(String category, String sessionId, ISessionProxy proxy) {
        this.category = category;
        this.sessionId = sessionId;
        this.proxy = proxy;
    }

    public void start(CastContextImpl castContext, CastDevice castDevice, String routeId, Bundle routeInfoExtra) throws RemoteException {
        this.castContext = castContext;
        this.castDevice = castDevice;
        this.routeInfoExtra = routeInfoExtra;
        this.routeId = routeId;

        this.mIsConnecting = true;
        this.mIsConnected = false;
        this.castContext.getSessionManagerImpl().onSessionStarting(this);
        this.proxy.start(routeInfoExtra);
    }

    public void onApplicationConnectionSuccess(ApplicationMetadata applicationMetadata, String applicationStatus, String sessionId, boolean wasLaunched) {
        this.mIsConnecting = false;
        this.mIsConnected = true;
        this.castContext.getSessionManagerImpl().onSessionStarted(this, sessionId);
        try {
            this.castContext.getRouter().selectRouteById(this.getRouteId());
        } catch (RemoteException ex) {
            Log.e(TAG, "Error calling selectRouteById: " + ex.getMessage());
        }
    }

    public void onApplicationConnectionFailure(int statusCode) {
        this.mIsConnecting = false;
        this.mIsConnected = false;
        this.routeId = null;
        this.castContext = null;
        this.castDevice = null;
        this.routeInfoExtra = null;
        this.castContext.getSessionManagerImpl().onSessionStartFailed(this, statusCode);
        try {
            this.castContext.getRouter().selectDefaultRoute();
        } catch (RemoteException ex) {
            Log.e(TAG, "Error calling selectDefaultRoute: " + ex.getMessage());
        }
    }

    public void onRouteSelected(Bundle extras) {
    }

    public CastSessionImpl getCastSession() {
        return this.castSession;
    }

    public void setCastSession(CastSessionImpl castSession) {
        this.castSession = castSession;
    }

    public ISessionProxy getSessionProxy() {
        return this.proxy;
    }

    public IObjectWrapper getWrappedSession() throws RemoteException {
        if (this.proxy == null) {
            return ObjectWrapper.wrap(null);
        }
        return this.proxy.getWrappedSession();
    }

    @Override
    public String getCategory() {
        return this.category;
    }

    @Override
    public String getSessionId() {
        return this.sessionId;
    }

    @Override
    public String getRouteId() {
        return this.routeId;
    }

    @Override
    public boolean isConnected() {
        return this.mIsConnected;
    }

    @Override
    public boolean isConnecting() {
        return this.mIsConnecting;
    }

    @Override
    public boolean isDisconnecting() {
        Log.d(TAG, "unimplemented Method: isDisconnecting");
        return false;
    }

    @Override
    public boolean isDisconnected() {
        Log.d(TAG, "unimplemented Method: isDisconnected");
        return false;
    }

    @Override
    public boolean isResuming() {
        Log.d(TAG, "unimplemented Method: isResuming");
        return false;
    }

    @Override
    public boolean isSuspended() {
        Log.d(TAG, "unimplemented Method: isSuspended");
        return false;
    }

    @Override
    public void notifySessionStarted(String sessionId) {
        Log.d(TAG, "unimplemented Method: notifySessionStarted");
    }

    @Override
    public void notifyFailedToStartSession(int error) {
        Log.d(TAG, "unimplemented Method: notifyFailedToStartSession");
    }

    @Override
    public void notifySessionEnded(int error) {
        Log.d(TAG, "unimplemented Method: notifySessionEnded");
    }

    @Override
    public void notifySessionResumed(boolean wasSuspended) {
        Log.d(TAG, "unimplemented Method: notifySessionResumed");
    }

    @Override
    public void notifyFailedToResumeSession(int error) {
        Log.d(TAG, "unimplemented Method: notifyFailedToResumeSession");
    }

    @Override
    public void notifySessionSuspended(int reason) {
        Log.d(TAG, "unimplemented Method: notifySessionSuspended");
    }

    @Override
    public IObjectWrapper getWrappedObject() {
        return ObjectWrapper.wrap(this);
    }
}
