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
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.framework.ISession;
import com.google.android.gms.cast.framework.ISessionProxy;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;

import org.microg.gms.common.Constants;

/**
 * Module side of a client library Session. The client library notifies about state changes using the notify* methods,
 * this class keeps track of the session state and forwards changes to the {@link SessionManagerImpl}.
 */
public class SessionImpl extends ISession.Stub {
    private static final String TAG = SessionImpl.class.getSimpleName();

    private static final int STATE_IDLE = 0;
    private static final int STATE_STARTING = 1;
    private static final int STATE_RESUMING = 2;
    private static final int STATE_CONNECTED = 3;
    private static final int STATE_SUSPENDED = 4;
    private static final int STATE_ENDING = 5;
    private static final int STATE_ENDED = 6;

    // Values of getSessionStartType()
    private static final int START_TYPE_NEW = 0;
    private static final int START_TYPE_RESUMED = 1;

    private final String category;
    private String sessionId;
    private final ISessionProxy proxy;

    private CastSessionImpl castSession;

    private CastContextImpl castContext;
    private CastDevice castDevice;
    private Bundle routeInfoExtra;
    private String routeId;

    private int state = STATE_IDLE;
    private int startType = START_TYPE_NEW;

    public SessionImpl(String category, String sessionId, ISessionProxy proxy) {
        this.category = category;
        this.sessionId = sessionId;
        this.proxy = proxy;
    }

    public void start(CastContextImpl castContext, CastDevice castDevice, String routeId, Bundle routeInfoExtra) throws RemoteException {
        attach(castContext, castDevice, routeId, routeInfoExtra);
        this.state = STATE_STARTING;
        this.startType = START_TYPE_NEW;
        this.proxy.onStarting(routeInfoExtra);
        this.castContext.getSessionManagerImpl().onSessionStarting(this);
        if (this.state != STATE_STARTING) {
            // A listener ended the session from onSessionStarting
            Log.d(TAG, "Session ended while starting");
            return;
        }
        this.proxy.start(routeInfoExtra);
    }

    public void resume(CastContextImpl castContext, CastDevice castDevice, String routeId, Bundle routeInfoExtra) throws RemoteException {
        attach(castContext, castDevice, routeId, routeInfoExtra);
        this.state = STATE_RESUMING;
        this.startType = START_TYPE_RESUMED;
        this.proxy.onResuming(routeInfoExtra);
        this.castContext.getSessionManagerImpl().onSessionResuming(this, sessionId);
        if (this.state != STATE_RESUMING) {
            // A listener ended the session from onSessionResuming
            Log.d(TAG, "Session ended while resuming");
            return;
        }
        this.proxy.resume(routeInfoExtra);
    }

    public void end(boolean stopCasting) throws RemoteException {
        if (state == STATE_ENDING || state == STATE_ENDED) return;
        this.state = STATE_ENDING;
        if (castContext != null) castContext.getSessionManagerImpl().onSessionEnding(this);
        this.proxy.end(stopCasting);
    }

    private void attach(CastContextImpl castContext, CastDevice castDevice, String routeId, Bundle routeInfoExtra) {
        this.castContext = castContext;
        this.castDevice = castDevice;
        this.routeId = routeId;
        this.routeInfoExtra = routeInfoExtra;
    }

    public void onRouteInfoUpdated(Bundle routeInfoExtra) {
        CastDevice castDevice = CastDevice.getFromBundle(routeInfoExtra);
        if (castDevice == null) return;
        this.castDevice = castDevice;
        this.routeInfoExtra = routeInfoExtra;
        try {
            this.proxy.onRouteInfoUpdated(routeInfoExtra);
        } catch (RemoteException e) {
            Log.d(TAG, "Remote exception calling onRouteInfoUpdated: " + e.getMessage());
        }
    }

    private void selectDefaultRouteIfSelected() {
        CastContextImpl castContext = this.castContext;
        String routeId = this.routeId;
        if (castContext == null || routeId == null) return;
        castContext.runOnMainThread(() -> {
            try {
                IMediaRouter router = castContext.getRouter();
                if (TextUtils.equals(router.getSelectedRouteId(), routeId)) {
                    router.selectDefaultRoute();
                }
            } catch (RemoteException e) {
                Log.w(TAG, "Error unselecting route: " + e.getMessage());
            }
        });
    }

    private SessionManagerImpl getSessionManager() {
        return castContext == null ? null : castContext.getSessionManagerImpl();
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

    public CastDevice getCastDevice() {
        return castDevice;
    }

    public Bundle getRouteInfoExtra() {
        return routeInfoExtra;
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
        return state == STATE_CONNECTED;
    }

    @Override
    public boolean isConnecting() {
        return state == STATE_STARTING;
    }

    @Override
    public boolean isDisconnecting() {
        return state == STATE_ENDING;
    }

    @Override
    public boolean isDisconnected() {
        return state == STATE_IDLE || state == STATE_ENDED;
    }

    @Override
    public boolean isResuming() {
        return state == STATE_RESUMING;
    }

    @Override
    public boolean isSuspended() {
        return state == STATE_SUSPENDED;
    }

    @Override
    public void notifySessionStarted(String sessionId) {
        Log.d(TAG, "notifySessionStarted: " + sessionId);
        if (state != STATE_STARTING) {
            Log.w(TAG, "Ignoring session start in state " + state);
            return;
        }
        this.state = STATE_CONNECTED;
        this.sessionId = sessionId;
        SessionManagerImpl sessionManager = getSessionManager();
        if (sessionManager != null) sessionManager.onSessionStarted(this, sessionId);
    }

    @Override
    public void notifyFailedToStartSession(int error) {
        Log.d(TAG, "notifyFailedToStartSession: " + error);
        if (state != STATE_STARTING) return;
        this.state = STATE_ENDED;
        SessionManagerImpl sessionManager = getSessionManager();
        if (sessionManager != null) sessionManager.onSessionStartFailed(this, error);
        selectDefaultRouteIfSelected();
    }

    @Override
    public void notifySessionEnded(int error) {
        Log.d(TAG, "notifySessionEnded: " + error);
        if (state == STATE_ENDED || state == STATE_IDLE) return;
        SessionManagerImpl sessionManager = getSessionManager();
        if (state != STATE_ENDING && sessionManager != null) {
            this.state = STATE_ENDING;
            sessionManager.onSessionEnding(this);
        }
        this.state = STATE_ENDED;
        if (sessionManager != null) sessionManager.onSessionEnded(this, error);
        selectDefaultRouteIfSelected();
    }

    @Override
    public void notifySessionResumed(boolean wasSuspended) {
        Log.d(TAG, "notifySessionResumed: " + wasSuspended);
        if (state != STATE_RESUMING && state != STATE_SUSPENDED) {
            Log.w(TAG, "Ignoring session resume in state " + state);
            return;
        }
        this.state = STATE_CONNECTED;
        SessionManagerImpl sessionManager = getSessionManager();
        if (sessionManager != null) sessionManager.onSessionResumed(this, wasSuspended);
    }

    @Override
    public void notifyFailedToResumeSession(int error) {
        Log.d(TAG, "notifyFailedToResumeSession: " + error);
        if (state != STATE_RESUMING) return;
        this.state = STATE_ENDED;
        SessionManagerImpl sessionManager = getSessionManager();
        if (sessionManager != null) sessionManager.onSessionResumeFailed(this, error);
        selectDefaultRouteIfSelected();
    }

    @Override
    public void notifySessionSuspended(int reason) {
        Log.d(TAG, "notifySessionSuspended: " + reason);
        if (state != STATE_CONNECTED) return;
        this.state = STATE_SUSPENDED;
        SessionManagerImpl sessionManager = getSessionManager();
        if (sessionManager != null) sessionManager.onSessionSuspended(this, reason);
    }

    @Override
    public int getSupportedVersion() {
        return Constants.GMS_VERSION_CODE;
    }

    @Override
    public int getSessionStartType() {
        return startType;
    }

    @Override
    public IObjectWrapper getWrappedObject() {
        return ObjectWrapper.wrap(this);
    }
}
