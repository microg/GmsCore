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
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.ICastStateListener;
import com.google.android.gms.cast.framework.ISession;
import com.google.android.gms.cast.framework.ISessionManager;
import com.google.android.gms.cast.framework.ISessionManagerListener;
import com.google.android.gms.cast.framework.ISessionProvider;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

public class SessionManagerImpl extends ISessionManager.Stub {
    private static final String TAG = SessionManagerImpl.class.getSimpleName();

    private final CastContextImpl castContext;

    private final Set<ISessionManagerListener> sessionManagerListeners = new HashSet<>();
    private final Set<ICastStateListener> castStateListeners = new HashSet<>();

    // Keyed by routeId for quick lookup when a route is selected/unselected.
    private final Map<String, SessionImpl> routeSessions = new HashMap<>();

    private SessionImpl currentSession;

    private int castState = CastState.NO_DEVICES_AVAILABLE;

    public SessionManagerImpl(CastContextImpl castContext) {
        this.castContext = castContext;
    }

    // ---- ISessionManager ----

    @Override
    public IObjectWrapper getWrappedCurrentSession() throws RemoteException {
        if (this.currentSession == null) return ObjectWrapper.wrap(null);
        return this.currentSession.getWrappedSession();
    }

    /**
     * Ends the current session, disconnecting from the Cast device.
     *
     * @param b           unused (legacy parameter)
     * @param stopCasting if true, also stop the receiver application on the device
     */
    @Override
    public void endCurrentSession(boolean b, boolean stopCasting) throws RemoteException {
        if (currentSession == null) return;

        SessionImpl session = currentSession;
        onSessionEnding(session);

        try {
            session.getSessionProxy().end(stopCasting);
        } catch (RemoteException e) {
            Log.w(TAG, "Error calling proxy.end: " + e.getMessage());
        }

        currentSession = null;
        setCastState(CastState.NOT_CONNECTED);
        onSessionEnded(session, 0);
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

    /**
     * Called by the framework when the user taps a route in the Cast dialog.
     * Looks up the registered ISessionProvider for the route's control category and starts
     * a new session, or resumes an existing one if a session for this route already exists.
     */
    @Override
    public void startSession(Bundle params) {
        String routeId = params.getString("CAST_INTENT_TO_CAST_ROUTE_ID_KEY");
        String sessionId = params.getString("CAST_INTENT_TO_CAST_SESSION_ID_KEY");
        Bundle routeInfoExtra = params.getBundle("CAST_INTENT_TO_CAST_ROUTE_INFO_EXTRA_KEY");
        String category = params.getString("CAST_INTENT_TO_CAST_ROUTE_CATEGORY_KEY");

        if (routeId == null) {
            Log.e(TAG, "startSession: missing routeId");
            return;
        }

        // Determine the CastDevice for this route so the session can report it.
        CastDevice castDevice = null;
        if (routeInfoExtra != null) {
            castDevice = CastDevice.getFromBundle(routeInfoExtra);
        }

        // Look up the session provider for this category.
        ISessionProvider provider = null;
        if (category != null) {
            provider = castContext.getSessionProviders().get(category);
        }
        if (provider == null) {
            provider = castContext.defaultSessionProvider;
        }

        if (provider == null) {
            Log.e(TAG, "startSession: no ISessionProvider found for category=" + category);
            return;
        }

        // Resume an existing session for this route if one already exists.
        SessionImpl existing = routeSessions.get(routeId);
        if (existing != null && sessionId != null) {
            resumeSession(existing, routeId, sessionId, routeInfoExtra);
            return;
        }

        // Create a new session via the provider.
        try {
            com.google.android.gms.dynamic.IObjectWrapper proxy = provider.getSession(sessionId);
            if (proxy == null) {
                Log.e(TAG, "startSession: provider returned null session");
                return;
            }
            // The provider returns an ISession, but we need the concrete SessionImpl.
            // Unwrap: our CastSessionProvider always returns a SessionImpl wrapped in ObjectWrapper.
            Object unwrapped = com.google.android.gms.dynamic.ObjectWrapper.unwrap(
                    proxy);
            if (!(unwrapped instanceof SessionImpl)) {
                Log.e(TAG, "startSession: provider did not return a SessionImpl");
                return;
            }
            SessionImpl session = (SessionImpl) unwrapped;
            routeSessions.put(routeId, session);
            session.start(castContext, castDevice, routeId, routeInfoExtra != null
                    ? routeInfoExtra : new Bundle());
        } catch (RemoteException e) {
            Log.e(TAG, "startSession: RemoteException: " + e.getMessage());
        }
    }

    private void resumeSession(SessionImpl session, String routeId, String sessionId,
            Bundle routeInfoExtra) {
        onSessionResuming(session, sessionId);
        try {
            session.getSessionProxy().resume(routeInfoExtra != null ? routeInfoExtra : new Bundle());
        } catch (RemoteException e) {
            Log.e(TAG, "resumeSession: RemoteException: " + e.getMessage());
        }
    }

    // ---- Internal callbacks from SessionImpl ----

    private void setCastState(int castState) {
        this.castState = castState;
        notifyCastStateChanged();
    }

    private void notifyCastStateChanged() {
        for (ICastStateListener listener : castStateListeners) {
            try {
                listener.onCastStateChanged(this.castState);
            } catch (RemoteException e) {
                Log.w(TAG, "onCastStateChanged: " + e.getMessage());
            }
        }
    }

    public void onSessionStarting(SessionImpl session) {
        setCastState(CastState.CONNECTING);
        for (ISessionManagerListener listener : sessionManagerListeners) {
            try {
                listener.onSessionStarting(session.getSessionProxy().getWrappedSession());
            } catch (RemoteException e) {
                Log.w(TAG, "onSessionStarting: " + e.getMessage());
            }
        }
    }

    public void onSessionStarted(SessionImpl session, String sessionId) {
        this.currentSession = session;
        setCastState(CastState.CONNECTED);
        for (ISessionManagerListener listener : sessionManagerListeners) {
            try {
                listener.onSessionStarted(session.getSessionProxy().getWrappedSession(), sessionId);
            } catch (RemoteException e) {
                Log.w(TAG, "onSessionStarted: " + e.getMessage());
            }
        }
    }

    public void onSessionStartFailed(SessionImpl session, int error) {
        this.currentSession = null;
        setCastState(CastState.NOT_CONNECTED);
        for (ISessionManagerListener listener : sessionManagerListeners) {
            try {
                listener.onSessionStartFailed(session.getSessionProxy().getWrappedSession(), error);
            } catch (RemoteException e) {
                Log.w(TAG, "onSessionStartFailed: " + e.getMessage());
            }
        }
    }

    public void onSessionEnding(SessionImpl session) {
        for (ISessionManagerListener listener : sessionManagerListeners) {
            try {
                listener.onSessionEnding(session.getSessionProxy().getWrappedSession());
            } catch (RemoteException e) {
                Log.w(TAG, "onSessionEnding: " + e.getMessage());
            }
        }
    }

    public void onSessionEnded(SessionImpl session, int error) {
        this.currentSession = null;
        routeSessions.values().remove(session);
        setCastState(CastState.NOT_CONNECTED);
        for (ISessionManagerListener listener : sessionManagerListeners) {
            try {
                listener.onSessionEnded(session.getSessionProxy().getWrappedSession(), error);
            } catch (RemoteException e) {
                Log.w(TAG, "onSessionEnded: " + e.getMessage());
            }
        }
    }

    public void onSessionResuming(SessionImpl session, String sessionId) {
        for (ISessionManagerListener listener : sessionManagerListeners) {
            try {
                listener.onSessionResuming(session.getSessionProxy().getWrappedSession(), sessionId);
            } catch (RemoteException e) {
                Log.w(TAG, "onSessionResuming: " + e.getMessage());
            }
        }
    }

    public void onSessionResumed(SessionImpl session, boolean wasSuspended) {
        setCastState(CastState.CONNECTED);
        for (ISessionManagerListener listener : sessionManagerListeners) {
            try {
                listener.onSessionResumed(session.getSessionProxy().getWrappedSession(), wasSuspended);
            } catch (RemoteException e) {
                Log.w(TAG, "onSessionResumed: " + e.getMessage());
            }
        }
    }

    public void onSessionResumeFailed(SessionImpl session, int error) {
        this.currentSession = null;
        setCastState(CastState.NOT_CONNECTED);
        for (ISessionManagerListener listener : sessionManagerListeners) {
            try {
                listener.onSessionResumeFailed(session.getSessionProxy().getWrappedSession(), error);
            } catch (RemoteException e) {
                Log.w(TAG, "onSessionResumeFailed: " + e.getMessage());
            }
        }
    }

    public void onSessionSuspended(SessionImpl session, int reason) {
        setCastState(CastState.NOT_CONNECTED);
        for (ISessionManagerListener listener : sessionManagerListeners) {
            try {
                listener.onSessionSuspended(session.getSessionProxy().getWrappedSession(), reason);
            } catch (RemoteException e) {
                Log.w(TAG, "onSessionSuspended: " + e.getMessage());
            }
        }
    }

    /**
     * Called by {@link MediaRouterCallbackImpl} when the set of discovered Cast devices changes
     * between empty and non-empty. Drives the NO_DEVICES_AVAILABLE ↔ NOT_CONNECTED transition
     * so the Cast button appears/disappears in the app toolbar.
     */
    public void onDeviceAvailabilityChanged(boolean available) {
        if (currentSession != null) return; // Never downgrade while a session is active.
        setCastState(available ? CastState.NOT_CONNECTED : CastState.NO_DEVICES_AVAILABLE);
    }
}
