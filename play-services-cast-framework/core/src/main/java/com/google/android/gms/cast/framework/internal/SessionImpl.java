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
    private String routeId;

    // Connection state machine. Only one of these is true at a time.
    private boolean mIsConnecting = false;
    private boolean mIsConnected = false;
    private boolean mIsDisconnecting = false;
    private boolean mIsDisconnected = false;
    private boolean mIsSuspended = false;
    private boolean mIsResuming = false;

    public SessionImpl(String category, String sessionId, ISessionProxy proxy) {
        this.category = category;
        this.sessionId = sessionId;
        this.proxy = proxy;
    }

    public void start(CastContextImpl castContext, CastDevice castDevice,
            String routeId, Bundle routeInfoExtra) throws RemoteException {
        this.castContext = castContext;
        this.castDevice = castDevice;
        this.routeInfoExtra = routeInfoExtra;
        this.routeId = routeId;

        setConnecting();
        this.castContext.getSessionManagerImpl().onSessionStarting(this);
        this.proxy.start(routeInfoExtra);
    }

    public void onApplicationConnectionSuccess(ApplicationMetadata applicationMetadata,
            String applicationStatus, String sessionId, boolean wasLaunched) {
        setConnected();
        this.castContext.getSessionManagerImpl().onSessionStarted(this, sessionId);
        try {
            this.castContext.getRouter().selectRouteById(this.getRouteId());
        } catch (RemoteException ex) {
            Log.e(TAG, "Error calling selectRouteById: " + ex.getMessage());
        }
    }

    public void onApplicationConnectionFailure(int statusCode) {
        // Save reference before clearing so we can still notify after teardown.
        CastContextImpl ctx = this.castContext;

        setDisconnected();
        this.routeId = null;
        this.castContext = null;
        this.castDevice = null;
        this.routeInfoExtra = null;

        if (ctx == null) return;

        ctx.getSessionManagerImpl().onSessionStartFailed(this, statusCode);
        try {
            ctx.getRouter().selectDefaultRoute();
        } catch (RemoteException ex) {
            Log.e(TAG, "Error calling selectDefaultRoute: " + ex.getMessage());
        }
    }

    public void onDisconnected(int reason) {
        CastContextImpl ctx = this.castContext;

        setDisconnecting();
        if (ctx != null) {
            ctx.getSessionManagerImpl().onSessionEnding(this);
        }
        setDisconnected();
        this.castContext = null;
        this.castDevice = null;
        this.routeInfoExtra = null;

        if (ctx != null) {
            ctx.getSessionManagerImpl().onSessionEnded(this, reason);
            try {
                ctx.getRouter().selectDefaultRoute();
            } catch (RemoteException ex) {
                Log.e(TAG, "Error calling selectDefaultRoute: " + ex.getMessage());
            }
        }
    }

    // ---- ISession ----

    @Override
    public String getCategory() { return category; }

    @Override
    public String getSessionId() { return sessionId; }

    @Override
    public String getRouteId() { return routeId; }

    @Override
    public boolean isConnected() { return mIsConnected; }

    @Override
    public boolean isConnecting() { return mIsConnecting; }

    @Override
    public boolean isDisconnecting() { return mIsDisconnecting; }

    @Override
    public boolean isDisconnected() { return mIsDisconnected; }

    @Override
    public boolean isResuming() { return mIsResuming; }

    @Override
    public boolean isSuspended() { return mIsSuspended; }

    @Override
    public void notifySessionStarted(String sessionId) {
        setConnected();
        if (castContext != null) {
            castContext.getSessionManagerImpl().onSessionStarted(this, sessionId);
        }
    }

    @Override
    public void notifyFailedToStartSession(int error) {
        onApplicationConnectionFailure(error);
    }

    @Override
    public void notifySessionEnded(int error) {
        onDisconnected(error);
    }

    @Override
    public void notifySessionResumed(boolean wasSuspended) {
        setConnected();
        if (castContext != null) {
            castContext.getSessionManagerImpl().onSessionResumed(this, wasSuspended);
        }
    }

    @Override
    public void notifyFailedToResumeSession(int error) {
        setDisconnected();
        if (castContext != null) {
            castContext.getSessionManagerImpl().onSessionResumeFailed(this, error);
        }
    }

    @Override
    public void notifySessionSuspended(int reason) {
        setSuspended();
        if (castContext != null) {
            castContext.getSessionManagerImpl().onSessionSuspended(this, reason);
        }
    }

    @Override
    public IObjectWrapper getWrappedObject() { return ObjectWrapper.wrap(this); }

    // ---- Accessors ----

    public CastSessionImpl getCastSession() { return castSession; }

    public void setCastSession(CastSessionImpl castSession) { this.castSession = castSession; }

    public ISessionProxy getSessionProxy() { return proxy; }

    public IObjectWrapper getWrappedSession() throws RemoteException {
        if (proxy == null) return ObjectWrapper.wrap(null);
        return proxy.getWrappedSession();
    }

    public void onRouteSelected(Bundle extras) { /* reserved */ }

    // ---- State helpers ----

    private void clearState() {
        mIsConnecting = false;
        mIsConnected = false;
        mIsDisconnecting = false;
        mIsDisconnected = false;
        mIsSuspended = false;
        mIsResuming = false;
    }

    private void setConnecting() {
        clearState();
        mIsConnecting = true;
    }

    private void setConnected() {
        clearState();
        mIsConnected = true;
    }

    private void setDisconnecting() {
        clearState();
        mIsDisconnecting = true;
    }

    private void setDisconnected() {
        clearState();
        mIsDisconnected = true;
    }

    private void setSuspended() {
        clearState();
        mIsSuspended = true;
    }
}
