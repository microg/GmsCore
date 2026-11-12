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

import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.ICastStateListener;
import com.google.android.gms.cast.framework.ISessionManager;
import com.google.android.gms.cast.framework.ISessionManagerListener;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class SessionManagerImpl extends ISessionManager.Stub {
    private static final String TAG = SessionManagerImpl.class.getSimpleName();

    private CastContextImpl castContext;

    // Fix: listeners are added from binder threads and iterated elsewhere,
    // so use thread-safe sets instead of HashSet.
    private final Set<ISessionManagerListener> sessionManagerListeners =
            new CopyOnWriteArraySet<ISessionManagerListener>();
    private final Set<ICastStateListener> castStateListeners =
            new CopyOnWriteArraySet<ICastStateListener>();

    private final Map<String, SessionImpl> routeSessions = new HashMap<String, SessionImpl>();
    private volatile SessionImpl currentSession;
    private volatile int castState = CastState.NO_DEVICES_AVAILABLE;

    private String pendingRouteId;
    private String pendingSessionId;

    public SessionManagerImpl(CastContextImpl castContext) {
        this.castContext = castContext;
    }

    @Override
    public IObjectWrapper getWrappedCurrentSession() throws RemoteException {
        SessionImpl session = this.currentSession;
        if (session == null) {
            return ObjectWrapper.wrap(null);
        }
        return session.getWrappedSession();
    }

    @Override
    public void endCurrentSession(boolean b, boolean stopCasting) throws RemoteException {
        // Fix: this was an empty stub.
        SessionImpl session = this.currentSession;
        if (session == null) {
            Log.d(TAG, "endCurrentSession: no current session");
            return;
        }
        // TODO: if stopCasting is true, also stop the app on the Chromecast.
        this.onSessionEnding(session);
        this.onSessionEnded(session, 0);
    }

    @Override
    public void addSessionManagerListener(ISessionManagerListener listener) {
        this.sessionManagerListeners.add(listener);
    }

    @Override
    public void removeSessionManagerListener(ISessionManagerListener listener) {
        this.sessionManagerListeners.remove(listener);
    }

    @Override
    public void addCastStateListener(ICastStateListener listener) {
        this.castStateListeners.add(listener);
    }

    @Override
    public void removeCastStateListener(ICastStateListener listener) {
        this.castStateListeners.remove(listener);
    }

    @Override
    public IObjectWrapper getWrappedThis() throws RemoteException {
        return ObjectWrapper.wrap(this);
    }

    @Override
    public int getCastState() {
        return this.castState;
    }

    @Override
    public void startSession(Bundle params) {
        String routeId = params.getString("CAST_INTENT_TO_CAST_ROUTE_ID_KEY");
        String sessionId = params.getString("CAST_INTENT_TO_CAST_SESSION_ID_KEY");
        Log.d(TAG, "startSession: route=" + routeId + " session=" + sessionId);
        this.pendingRouteId = routeId;
        this.pendingSessionId = sessionId;
        if (routeId != null) {
            this.onRouteSelected(routeId, params);
        }
    }

    public void onRouteSelected(String routeId, Bundle extras) {
        Log.d(TAG, "onRouteSelected: " + routeId);
        this.pendingRouteId = routeId;
        // TODO (needs SessionImpl.java): create a SessionImpl for this route,
        // connect it to the Chromecast, then call startSessionFor(...) below.
    }

    /**
     * NEW: call this when the list of Chromecast devices changes.
     * It drives the visibility of the Cast button in apps.
     */
    public void onDeviceAvailabilityChanged(boolean devicesAvailable) {
        if (this.currentSession != null) {
            return; // a session is active, keep its state
        }
        this.setCastState(devicesAvailable
                ? CastState.NOT_CONNECTED
                : CastState.NO_DEVICES_AVAILABLE);
    }

    /**
     * NEW: whoever creates the SessionImpl and connects it calls this
     * to publish the whole start sequence to the app.
     */
    public void startSessionFor(String routeId, SessionImpl session, String sessionId) {
        this.routeSessions.put(routeId, session);
        this.onSessionStarting(session);
        this.onSessionStarted(session, sessionId);
    }

    /** NEW: call this when connecting failed. */
    public void failSessionStart(String routeId, SessionImpl session, int error) {
        this.routeSessions.remove(routeId);
        this.onSessionStartFailed(session, error);
    }

    private void setCastState(int castState) {
        this.castState = castState;
        this.onCastStateChanged();
    }

    public void onCastStateChanged() {
        for (ICastStateListener listener : this.castStateListeners) {
            try {
                listener.onCastStateChanged(this.castState);
            } catch (RemoteException e) {
                Log.d(TAG, "Remote exception calling onCastStateChanged: " + e.getMessage());
            }
        }
    }

    public void onSessionStarting(SessionImpl session) {
        this.setCastState(CastState.CONNECTING);
        for (ISessionManagerListener listener : this.sessionManagerListeners) {
            try {
                listener.onSessionStarting(session.getSessionProxy().getWrappedSession());
            } catch (RemoteException e) {
                Log.d(TAG, "Remote exception calling onSessionStarting: " + e.getMessage());
            }
        }
    }

    public void onSessionStartFailed(SessionImpl session, int error) {
        this.currentSession = null;
        this.setCastState(CastState.NOT_CONNECTED);
        for (ISessionManagerListener listener : this.sessionManagerListeners) {
            try {
                listener.onSessionStartFailed(session.getSessionProxy().getWrappedSession(), error);
            } catch (RemoteException e) {
                Log.d(TAG, "Remote exception calling onSessionStartFailed: " + e.getMessage());
            }
        }
    }

    public void onSessionStarted(SessionImpl session, String sessionId) {
        this.currentSession = session;
        this.setCastState(CastState.CONNECTED);
        for (ISessionManagerListener listener : this.sessionManagerListeners) {
            try {
                listener.onSessionStarted(session.getSessionProxy().getWrappedSession(), sessionId);
            } catch (RemoteException e) {
                Log.d(TAG, "Remote exception calling onSessionStarted: " + e.getMessage());
            }
        }
    }

    public void onSessionResumed(SessionImpl session, boolean wasSuspended) {
        this.setCastState(CastState.CONNECTED);
        for (ISessionManagerListener listener : this.sessionManagerListeners) {
            try {
                listener.onSessionResumed(session.getSessionProxy().getWrappedSession(), wasSuspended);
            } catch (RemoteException e) {
                Log.d(TAG, "Remote exception calling onSessionResumed: " + e.getMessage());
            }
        }
    }

    public void onSessionEnding(SessionImpl session) {
        for (ISessionManagerListener listener : this.sessionManagerListeners) {
            try {
                listener.onSessionEnding(session.getSessionProxy().getWrappedSession());
            } catch (RemoteException e) {
                Log.d(TAG, "Remote exception calling onSessionEnding: " + e.getMessage());
            }
        }
    }

    public void onSessionEnded(SessionImpl session, int error) {
        this.currentSession = null;
        this.setCastState(CastState.NOT_CONNECTED);
        for (ISessionManagerListener listener : this.sessionManagerListeners) {
            try {
                listener.onSessionEnded(session.getSessionProxy().getWrappedSession(), error);
            } catch (RemoteException e) {
                Log.d(TAG, "Remote exception calling onSessionEnded: " + e.getMessage());
            }
        }
    }

    public void onSessionResuming(SessionImpl session, String sessionId) {
        for (ISessionManagerListener listener : this.sessionManagerListeners) {
            try {
                listener.onSessionResuming(session.getSessionProxy().getWrappedSession(), sessionId);
            } catch (RemoteException e) {
                Log.d(TAG, "Remote exception calling onSessionResuming: " + e.getMessage());
            }
        }
    }

    public void onSessionResumeFailed(SessionImpl session, int error) {
        this.currentSession = null;
        this.setCastState(CastState.NOT_CONNECTED);
        for (ISessionManagerListener listener : this.sessionManagerListeners) {
            try {
                listener.onSessionResumeFailed(session.getSessionProxy().getWrappedSession(), error);
            } catch (RemoteException e) {
                Log.d(TAG, "Remote exception calling onSessionResumeFailed: " + e.getMessage());
            }
        }
    }

    public void onSessionSuspended(SessionImpl session, int reason) {
        this.setCastState(CastState.NOT_CONNECTED);
        for (ISessionManagerListener listener : this.sessionManagerListeners) {
            try {
                listener.onSessionSuspended(session.getSessionProxy().getWrappedSession(), reason);
            } catch (RemoteException e) {
                Log.d(TAG, "Remote exception calling onSessionSuspended: " + e.getMessage());
            }
        }
    }
}
