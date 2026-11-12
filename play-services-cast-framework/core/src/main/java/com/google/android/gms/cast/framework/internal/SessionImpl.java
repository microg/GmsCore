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
import android.util.Log;

import com.google.android.gms.cast.framework.ISession;
import com.google.android.gms.cast.framework.ISessionProxy;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;

/**
 * Module side of {@code Session}. Keeps the state of a single session and forwards lifecycle changes to
 * {@link SessionManagerImpl}.
 * <p>
 * For Cast sessions the state changes are driven by {@link CastSessionImpl}, for custom sessions the client reports
 * them through the {@code notify*()} methods.
 */
public class SessionImpl extends ISession.Stub {
    private static final String TAG = "GmsCastSession";

    private static final int STATE_NEW = 0;
    private static final int STATE_STARTING = 1;
    private static final int STATE_RESUMING = 2;
    private static final int STATE_CONNECTED = 3;
    private static final int STATE_SUSPENDED = 4;
    private static final int STATE_ENDING = 5;
    private static final int STATE_ENDED = 6;

    private final String category;
    private final ISessionProxy proxy;
    private String sessionId;

    private CastContextImpl castContext;
    private CastSessionImpl castSession;
    private Bundle routeInfoExtra;
    private String routeId = null;
    private int state = STATE_NEW;

    public SessionImpl(CastContextImpl castContext, String category, String sessionId, ISessionProxy proxy) {
        this.castContext = castContext;
        this.category = category;
        this.sessionId = sessionId;
        this.proxy = proxy;
    }

    private SessionManagerImpl getSessionManager() {
        CastContextImpl castContext = this.castContext;
        return castContext == null ? null : castContext.getSessionManagerImpl();
    }

    /**
     * Starts this session on the given route. If {@code joinSessionId} is set, the receiver application is expected to
     * be running already and is joined instead of launched.
     */
    public void start(CastContextImpl castContext, String routeId, Bundle routeInfoExtra, String joinSessionId) {
        this.castContext = castContext;
        this.routeInfoExtra = routeInfoExtra;
        this.routeId = routeId;

        boolean resume = joinSessionId != null;
        this.state = resume ? STATE_RESUMING : STATE_STARTING;
        try {
            if (resume) {
                this.sessionId = joinSessionId;
                this.proxy.onResuming(routeInfoExtra);
                castContext.getSessionManagerImpl().onSessionResuming(this, joinSessionId);
                this.proxy.resume(routeInfoExtra);
            } else {
                this.proxy.onStarting(routeInfoExtra);
                castContext.getSessionManagerImpl().onSessionStarting(this);
                this.proxy.start(routeInfoExtra);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to start session", e);
            if (resume) {
                notifyFailedToResumeSession(CommonStatusCodes.INTERNAL_ERROR);
            } else {
                notifyFailedToStartSession(CommonStatusCodes.INTERNAL_ERROR);
            }
        }
    }

    /**
     * Asks the client to end the session. The client is expected to report back using {@link #notifySessionEnded}.
     */
    public void end(boolean stopCasting) {
        if (isDisconnected() || isDisconnecting()) return;
        this.state = STATE_ENDING;
        SessionManagerImpl sessionManager = getSessionManager();
        if (sessionManager != null) sessionManager.onSessionEnding(this);
        try {
            this.proxy.end(stopCasting);
        } catch (Exception e) {
            Log.w(TAG, "Failed to end session", e);
            // Client won't report back, don't keep the session around forever
            notifySessionEnded(CommonStatusCodes.INTERNAL_ERROR);
        }
    }

    public void onRouteInfoUpdated(Bundle routeInfoExtra) {
        this.routeInfoExtra = routeInfoExtra;
        try {
            this.proxy.onRouteInfoUpdated(routeInfoExtra);
        } catch (Exception e) {
            Log.w(TAG, "Failed to update route info", e);
        }
    }

    public CastSessionImpl getCastSession() {
        return this.castSession;
    }

    public void setCastSession(CastSessionImpl castSession) {
        this.castSession = castSession;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public ISessionProxy getSessionProxy() {
        return this.proxy;
    }


    public IObjectWrapper getWrappedSession() throws android.os.RemoteException {
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
        return this.state == STATE_CONNECTED;
    }

    @Override
    public boolean isConnecting() {
        return this.state == STATE_STARTING;
    }

    @Override
    public boolean isDisconnecting() {
        return this.state == STATE_ENDING;
    }

    @Override
    public boolean isDisconnected() {
        return this.state == STATE_NEW || this.state == STATE_ENDED;
    }

    @Override
    public boolean isResuming() {
        return this.state == STATE_RESUMING;
    }

    @Override
    public boolean isSuspended() {
        return this.state == STATE_SUSPENDED;
    }

    @Override
    public void notifySessionStarted(String sessionId) {
        if (this.state != STATE_STARTING) {
            Log.d(TAG, "Ignoring notifySessionStarted in state " + this.state);
            return;
        }
        this.sessionId = sessionId;
        this.state = STATE_CONNECTED;
        SessionManagerImpl sessionManager = getSessionManager();
        if (sessionManager != null) sessionManager.onSessionStarted(this, sessionId);
    }

    @Override
    public void notifyFailedToStartSession(int error) {
        if (this.state != STATE_STARTING) {
            Log.d(TAG, "Ignoring notifyFailedToStartSession in state " + this.state);
            return;
        }
        this.state = STATE_ENDED;
        SessionManagerImpl sessionManager = getSessionManager();
        if (sessionManager != null) sessionManager.onSessionStartFailed(this, error);
    }

    @Override
    public void notifySessionEnded(int error) {
        if (this.state == STATE_ENDED || this.state == STATE_NEW) return;
        if (this.state == STATE_STARTING) {
            notifyFailedToStartSession(error);
            return;
        }
        if (this.state == STATE_RESUMING) {
            notifyFailedToResumeSession(error);
            return;
        }
        this.state = STATE_ENDED;
        SessionManagerImpl sessionManager = getSessionManager();
        if (sessionManager != null) sessionManager.onSessionEnded(this, error);
    }

    @Override
    public void notifySessionResumed(boolean wasSuspended) {
        if (this.state != STATE_RESUMING && this.state != STATE_SUSPENDED) {
            Log.d(TAG, "Ignoring notifySessionResumed in state " + this.state);
            return;
        }
        this.state = STATE_CONNECTED;
        SessionManagerImpl sessionManager = getSessionManager();
        if (sessionManager != null) sessionManager.onSessionResumed(this, wasSuspended);
    }

    @Override
    public void notifyFailedToResumeSession(int error) {
        if (this.state != STATE_RESUMING) {
            Log.d(TAG, "Ignoring notifyFailedToResumeSession in state " + this.state);
            return;
        }
        this.state = STATE_ENDED;
        SessionManagerImpl sessionManager = getSessionManager();
        if (sessionManager != null) sessionManager.onSessionResumeFailed(this, error);
    }

    @Override
    public void notifySessionSuspended(int reason) {
        if (this.state != STATE_CONNECTED) {
            Log.d(TAG, "Ignoring notifySessionSuspended in state " + this.state);
            return;
        }
        this.state = STATE_SUSPENDED;
        SessionManagerImpl sessionManager = getSessionManager();
        if (sessionManager != null) sessionManager.onSessionSuspended(this, reason);
    }

    @Override
    public int getSupportedVersion() {
        return CastDynamiteModuleImpl.MODULE_VERSION;
    }

    @Override
    public int getSessionStartType() {
        // 0 stands for a session the user started by picking a route in this app. Reported for every session, as we
        // don't tell apart how a session came to be.
        return 0;
    }

    @Override
    public IObjectWrapper getWrappedObject() {
        return ObjectWrapper.wrap(this);
    }
}
