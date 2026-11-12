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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import androidx.mediarouter.media.MediaRouter;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.ICastStateListener;
import com.google.android.gms.cast.framework.ISessionManager;
import com.google.android.gms.cast.framework.ISessionManagerListener;
import com.google.android.gms.cast.framework.ISessionProvider;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;


/**
 * Module side of {@code SessionManager}: Starts a session when a Cast route gets selected in the client app, ends it
 * when the route gets unselected and reports the session lifecycle to the listeners of the client.
 * <p>
 * Everything but adding and removing listeners is expected to happen on the main thread of the client app. Calls
 * into the client are guarded broadly, as an exception of the client must not leave us in a state that makes no sense.
 */
public class SessionManagerImpl extends ISessionManager.Stub {
    private static final String TAG = "GmsCastSessionManager";

    private static final String KEY_ROUTE_ID = "CAST_INTENT_TO_CAST_ROUTE_ID_KEY";
    private static final String KEY_SESSION_ID = "CAST_INTENT_TO_CAST_SESSION_ID_KEY";

    // Where the running session is remembered, to pick it up again after the app process was killed
    private static final String PREFERENCES = "org.microg.gms.cast.framework.session";
    private static final String PREF_ROUTE_ID = "routeId";
    private static final String PREF_SESSION_ID = "sessionId";
    private static final String PREF_APPLICATION_ID = "applicationId";
    // How long to wait for the device of the remembered session to be discovered
    private static final long RESUME_DISCOVERY_TIMEOUT_MILLIS = 15000;

    private final CastContextImpl castContext;

    private final ListenerRegistry<ISessionManagerListener> sessionManagerListeners = new ListenerRegistry<>(TAG, ISessionManagerListener::getWrappedThis);
    private final ListenerRegistry<ICastStateListener> castStateListeners = new ListenerRegistry<>(TAG, ICastStateListener::getWrappedThis);

    private SessionImpl currentSession;
    // Session of a previous process, to be resumed once its route shows up
    private SessionReference savedSession;
    // Session to join instead of starting a new one once its route gets selected
    private SessionReference pendingJoin;
    private boolean devicesAvailable = false;
    private int castState = CastState.NO_DEVICES_AVAILABLE;

    public SessionManagerImpl(CastContextImpl castContext) {
        this.castContext = castContext;
    }

    @Override
    public IObjectWrapper getWrappedCurrentSession() throws RemoteException {
        if (this.currentSession == null) {
            return ObjectWrapper.wrap(null);
        }
        return this.currentSession.getWrappedSession();
    }

    @Override
    public void endCurrentSession(boolean b, boolean stopCasting) throws RemoteException {
        Log.d(TAG, "endCurrentSession: stopCasting=" + stopCasting);
        SessionImpl session = this.currentSession;
        if (session != null) {
            endSession(session, stopCasting);
        }
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
     * Invoked by the client to start (or join) a session on a route it learned about by other means than the user
     * picking it in the route chooser, e.g. from an intent.
     */
    @Override
    public void startSession(Bundle params) {
        if (params == null) return;
        String routeId = params.getString(KEY_ROUTE_ID);
        String sessionId = params.getString(KEY_SESSION_ID);
        Log.d(TAG, "startSession: routeId=" + routeId + " sessionId=" + sessionId);
        if (routeId == null) return;
        if (this.currentSession != null && routeId.equals(this.currentSession.getRouteId())) return;

        this.pendingJoin = new SessionReference(routeId, sessionId);
        try {
            IMediaRouter router = castContext.getRouter();
            if (routeId.equals(router.getSelectedRouteId())) {
                // Already selected, so there won't be a callback
                onRouteSelected(routeId, router.getRouteInfoExtrasById(routeId));
            } else {
                router.selectRouteById(routeId);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to select route " + routeId, e);
        }
    }

    /**
     * A session of the receiver application on the device behind a route.
     */
    private static final class SessionReference {
        final String routeId;
        final String sessionId;

        SessionReference(String routeId, String sessionId) {
            this.routeId = routeId;
            this.sessionId = sessionId;
        }
    }

    // region Resuming the session of a previous process

    private SharedPreferences getPreferences() {
        Context context = castContext.getContext();
        return context == null ? null : context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    private void saveSession(SessionImpl session) {
        try {
            SharedPreferences preferences = getPreferences();
            if (preferences == null || session.getRouteId() == null || session.getSessionId() == null) return;
            preferences.edit()
                    .putString(PREF_ROUTE_ID, session.getRouteId())
                    .putString(PREF_SESSION_ID, session.getSessionId())
                    .putString(PREF_APPLICATION_ID, castContext.getReceiverApplicationId())
                    .apply();
        } catch (Exception e) {
            Log.w(TAG, "Failed to save session", e);
        }
    }

    private void clearSavedSession() {
        this.savedSession = null;
        try {
            SharedPreferences preferences = getPreferences();
            if (preferences != null && preferences.contains(PREF_ROUTE_ID)) preferences.edit().clear().apply();
        } catch (Exception e) {
            Log.w(TAG, "Failed to clear saved session", e);
        }
    }

    /**
     * If the process was killed while a session was running, the receiver application keeps running. Join it again as
     * soon as its device is discovered, unless the client opted out. To be invoked on the main thread.
     */
    void resumeSavedSession() {
        CastOptions options = castContext.getOptions();
        if (options == null || !options.getResumeSavedSession() || this.currentSession != null) return;
        try {
            SharedPreferences preferences = getPreferences();
            if (preferences == null) return;
            String routeId = preferences.getString(PREF_ROUTE_ID, null);
            String sessionId = preferences.getString(PREF_SESSION_ID, null);
            if (routeId == null || sessionId == null) return;
            if (!TextUtils.equals(preferences.getString(PREF_APPLICATION_ID, null), castContext.getReceiverApplicationId())) {
                clearSavedSession();
                return;
            }
            Log.d(TAG, "Found saved session " + sessionId + ", waiting for route " + routeId);
            this.savedSession = new SessionReference(routeId, sessionId);
        } catch (Exception e) {
            Log.w(TAG, "Failed to read saved session", e);
            return;
        }
        castContext.getMainHandler().postDelayed(() -> {
            if (this.savedSession != null) {
                Log.d(TAG, "Device of saved session was not found, not resuming");
                this.savedSession = null;
            }
        }, RESUME_DISCOVERY_TIMEOUT_MILLIS);
        onRoutesChanged();
    }

    /**
     * To be invoked whenever the set of routes known to the MediaRouter of the client changed.
     */
    void onRoutesChanged() {
        SessionReference saved = this.savedSession;
        if (saved == null) return;
        if (this.currentSession != null) {
            this.savedSession = null;
            return;
        }
        try {
            if (CastDevice.getFromBundle(castContext.getRouter().getRouteInfoExtrasById(saved.routeId)) == null) return;
        } catch (Exception e) {
            return;
        }
        Log.d(TAG, "Resuming saved session " + saved.sessionId + " on route " + saved.routeId);
        Bundle params = new Bundle();
        params.putString(KEY_ROUTE_ID, saved.routeId);
        params.putString(KEY_SESSION_ID, saved.sessionId);
        this.savedSession = null;
        startSession(params);
    }

    // endregion

    // region Route events

    public void onRouteSelected(String routeId, Bundle extras) {
        if (routeId == null) return;
        // Whatever gets selected now takes precedence over a session we were waiting to resume
        this.savedSession = null;
        if (this.currentSession != null) {
            if (routeId.equals(this.currentSession.getRouteId())) {
                // Selection is reported through multiple callbacks, see MediaRouterCallbackImpl
                return;
            }
            Log.d(TAG, "Route changed while in session, ending previous session");
            endSession(this.currentSession, false);
            this.currentSession = null;
        }

        if (extras == null) {
            try {
                extras = castContext.getRouter().getRouteInfoExtrasById(routeId);
            } catch (Exception e) {
                Log.w(TAG, "Failed to get extras of route " + routeId, e);
            }
        }
        CastDevice castDevice = CastDevice.getFromBundle(extras);
        if (castDevice == null) {
            Log.d(TAG, "Selected route is not a cast route: " + routeId);
            return;
        }

        String joinSessionId = this.pendingJoin != null && routeId.equals(this.pendingJoin.routeId) ? this.pendingJoin.sessionId : null;
        this.pendingJoin = null;

        ISessionProvider provider = castContext.getDefaultSessionProvider();
        if (provider == null) {
            Log.w(TAG, "No session provider available, can't start session");
            return;
        }

        SessionImpl session;
        try {
            // The client creates its Session object here, which in turn calls back into
            // CastDynamiteModuleImpl.newSessionImpl() and hands us the result of that.
            Object unwrapped = ObjectWrapper.unwrap(provider.getSession(joinSessionId));
            if (!(unwrapped instanceof SessionImpl)) {
                Log.w(TAG, "Session provider returned unexpected session: " + unwrapped);
                unselectRoute(routeId);
                return;
            }
            session = (SessionImpl) unwrapped;
        } catch (Exception e) {
            Log.w(TAG, "Failed to create session", e);
            unselectRoute(routeId);
            return;
        }

        Log.d(TAG, "Starting session on " + castDevice.getFriendlyName() + " (route " + routeId + ")");
        this.currentSession = session;
        session.start(castContext, routeId, extras, joinSessionId);
    }

    public void onRouteUnselected(String routeId, int reason) {
        SessionImpl session = this.currentSession;
        if (session == null || routeId == null || !routeId.equals(session.getRouteId())) return;
        CastOptions options = castContext.getOptions();
        boolean stopCasting = reason == MediaRouter.UNSELECT_REASON_STOPPED
                || (options != null && options.getStopReceiverApplicationWhenEndingSession());
        endSession(session, stopCasting);
    }

    public void onRouteChanged(String routeId, Bundle extras) {
        SessionImpl session = this.currentSession;
        if (session == null || extras == null || !session.isConnected() || !TextUtils.equals(routeId, session.getRouteId())) return;
        session.onRouteInfoUpdated(extras);
    }

    public void onDeviceAvailabilityChanged(boolean available) {
        this.devicesAvailable = available;
        updateCastState();
    }

    // endregion

    private void endSession(SessionImpl session, boolean stopCasting) {
        if (session.isDisconnected() || session.isDisconnecting()) return;
        session.end(stopCasting);
    }

    /**
     * Makes the route chooser of the client show as disconnected again.
     */
    private void unselectRoute(String routeId) {
        if (routeId == null) return;
        try {
            IMediaRouter router = castContext.getRouter();
            if (routeId.equals(router.getSelectedRouteId())) {
                router.selectDefaultRoute();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to unselect route " + routeId, e);
        }
    }

    private void updateCastState() {
        int newState;
        SessionImpl session = this.currentSession;
        if (session != null && session.isConnected()) {
            newState = CastState.CONNECTED;
        } else if (session != null && (session.isConnecting() || session.isResuming() || session.isSuspended())) {
            // While suspended the connection to the device is being re-established
            newState = CastState.CONNECTING;
        } else if (devicesAvailable || session != null) {
            newState = CastState.NOT_CONNECTED;
        } else {
            newState = CastState.NO_DEVICES_AVAILABLE;
        }
        if (newState == this.castState) return;
        this.castState = newState;
        Log.d(TAG, "Cast state changed: " + CastState.toString(newState));

        this.castStateListeners.forEach("onCastStateChanged", listener -> listener.onCastStateChanged(newState));
    }

    private interface ListenerCall {
        void call(ISessionManagerListener listener, IObjectWrapper session) throws RemoteException;
    }

    private void notifyListeners(SessionImpl session, String what, ListenerCall call) {
        IObjectWrapper wrappedSession;
        try {
            wrappedSession = session.getWrappedSession();
        } catch (Exception e) {
            Log.w(TAG, "Failed to get wrapped session for " + what, e);
            return;
        }
        Log.d(TAG, what + " -> " + this.sessionManagerListeners.size() + " listeners");
        this.sessionManagerListeners.forEach(what, listener -> call.call(listener, wrappedSession));
    }

    // region Session lifecycle, invoked by SessionImpl

    public void onSessionStarting(SessionImpl session) {
        updateCastState();
        notifyListeners(session, "onSessionStarting", (listener, wrapped) -> listener.onSessionStarting(wrapped));
    }

    public void onSessionStartFailed(SessionImpl session, int error) {
        String routeId = session.getRouteId();
        if (this.currentSession == session) this.currentSession = null;
        updateCastState();
        notifyListeners(session, "onSessionStartFailed", (listener, wrapped) -> listener.onSessionStartFailed(wrapped, error));
        unselectRoute(routeId);
    }

    public void onSessionStarted(SessionImpl session, String sessionId) {
        this.currentSession = session;
        saveSession(session);
        updateCastState();
        notifyListeners(session, "onSessionStarted", (listener, wrapped) -> listener.onSessionStarted(wrapped, sessionId));
    }

    public void onSessionResuming(SessionImpl session, String sessionId) {
        updateCastState();
        notifyListeners(session, "onSessionResuming", (listener, wrapped) -> listener.onSessionResuming(wrapped, sessionId));
    }

    public void onSessionResumed(SessionImpl session, boolean wasSuspended) {
        this.currentSession = session;
        saveSession(session);
        updateCastState();
        notifyListeners(session, "onSessionResumed", (listener, wrapped) -> listener.onSessionResumed(wrapped, wasSuspended));
    }

    public void onSessionResumeFailed(SessionImpl session, int error) {
        String routeId = session.getRouteId();
        if (this.currentSession == session) this.currentSession = null;
        // What is saved belongs to the session that replaced this one, if there is one
        if (this.currentSession == null) clearSavedSession();
        updateCastState();
        notifyListeners(session, "onSessionResumeFailed", (listener, wrapped) -> listener.onSessionResumeFailed(wrapped, error));
        unselectRoute(routeId);
    }

    public void onSessionEnding(SessionImpl session) {
        notifyListeners(session, "onSessionEnding", (listener, wrapped) -> listener.onSessionEnding(wrapped));
    }

    public void onSessionEnded(SessionImpl session, int error) {
        String routeId = session.getRouteId();
        if (this.currentSession == session) this.currentSession = null;
        // What is saved belongs to the session that replaced this one, if there is one
        if (this.currentSession == null) clearSavedSession();
        updateCastState();
        notifyListeners(session, "onSessionEnded", (listener, wrapped) -> listener.onSessionEnded(wrapped, error));
        unselectRoute(routeId);
    }

    public void onSessionSuspended(SessionImpl session, int reason) {
        updateCastState();
        notifyListeners(session, "onSessionSuspended", (listener, wrapped) -> listener.onSessionSuspended(wrapped, reason));
    }

    // endregion
}
