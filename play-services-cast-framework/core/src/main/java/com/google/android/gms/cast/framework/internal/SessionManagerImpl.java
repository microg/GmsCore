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
import android.os.IInterface;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import androidx.mediarouter.media.MediaRouter;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.ICastStateListener;
import com.google.android.gms.cast.framework.ISessionManager;
import com.google.android.gms.cast.framework.ISessionManagerListener;
import com.google.android.gms.cast.framework.ISessionProvider;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

public class SessionManagerImpl extends ISessionManager.Stub {
    private static final String TAG = SessionManagerImpl.class.getSimpleName();

    private static final String KEY_ROUTE_ID = "CAST_INTENT_TO_CAST_ROUTE_ID_KEY";
    private static final String PREFERENCES_NAME = "com.google.android.gms.cast.framework.internal.session";
    private static final String PREF_ROUTE_ID = "route_id";
    private static final String PREF_SESSION_ID = "session_id";
    private static final String PREF_CATEGORY = "category";
    private static final long RESUME_TIMEOUT_MS = 10000;

    private final CastContextImpl castContext;

    // The client library wraps its listener in a new binder for every add and remove call, so listeners are keyed by
    // the client's listener object they wrap.
    private final Map<Object, ISessionManagerListener> sessionManagerListeners = new IdentityHashMap<>();
    private final Map<Object, ICastStateListener> castStateListeners = new IdentityHashMap<>();

    private SessionImpl currentSession;

    private int castState = CastState.NO_DEVICES_AVAILABLE;

    private String resumeRouteId;
    private String resumeSessionId;
    private long resumeDeadline;

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

    public SessionImpl getCurrentSession() {
        return currentSession;
    }

    @Override
    public void endCurrentSession(boolean b, boolean stopCasting) throws RemoteException {
        castContext.runOnMainThread(() -> endCurrentSessionInternal(stopCasting));
    }

    void endCurrentSessionInternal(boolean stopCasting) {
        if (currentSession == null) return;
        try {
            currentSession.end(stopCasting);
        } catch (RemoteException e) {
            Log.w(TAG, "Error ending session: " + e.getMessage());
        }
    }

    private static Object getListenerKey(IInterface listener, IObjectWrapper wrappedThis) {
        try {
            Object unwrapped = ObjectWrapper.unwrap(wrappedThis);
            if (unwrapped != null) return unwrapped;
        } catch (RuntimeException e) {
            Log.w(TAG, "Failed to unwrap listener: " + e.getMessage());
        }
        return listener.asBinder();
    }

    @Override
    public void addSessionManagerListener(ISessionManagerListener listener) throws RemoteException {
        if (listener != null) this.sessionManagerListeners.put(getListenerKey(listener, listener.getWrappedThis()), listener);
    }

    @Override
    public void removeSessionManagerListener(ISessionManagerListener listener) throws RemoteException {
        if (listener != null) this.sessionManagerListeners.remove(getListenerKey(listener, listener.getWrappedThis()));
    }

    @Override
    public void addCastStateListener(ICastStateListener listener) throws RemoteException {
        if (listener != null) this.castStateListeners.put(getListenerKey(listener, listener.getWrappedThis()), listener);
    }

    @Override
    public void removeCastStateListener(ICastStateListener listener) throws RemoteException {
        if (listener != null) this.castStateListeners.remove(getListenerKey(listener, listener.getWrappedThis()));
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
        if (params == null) return;
        String routeId = params.getString(KEY_ROUTE_ID);
        Log.d(TAG, "startSession: " + routeId);
        if (routeId == null) return;
        castContext.runOnMainThread(() -> {
            try {
                castContext.getRouter().selectRouteById(routeId);
            } catch (RemoteException e) {
                Log.w(TAG, "Error selecting route " + routeId + ": " + e.getMessage());
            }
        });
    }

    public void onRouteSelected(String routeId, Bundle extras) {
        onRouteSelected(castContext.getDefaultCategory(), routeId, extras);
    }

    /**
     * Called when a media route was selected, either by the user in the route chooser or using {@link #startSession}.
     *
     * @param category the control category of the session provider the selected route was matched for
     */
    public void onRouteSelected(String category, String routeId, Bundle extras) {
        boolean isCast = TextUtils.equals(category, castContext.getDefaultCategory());
        CastDevice castDevice = CastDevice.getFromBundle(extras);
        if (isCast && castDevice == null) {
            Log.d(TAG, "Selected route " + routeId + " is not a Cast device");
            return;
        }
        if (currentSession != null && !currentSession.isDisconnected()) {
            if (TextUtils.equals(currentSession.getRouteId(), routeId)) {
                // Also reached when the route matches the categories of multiple session providers.
                Log.d(TAG, "Route " + routeId + " already has a session");
                return;
            }
            endCurrentSessionInternal(castContext.getOptions().getStopReceiverApplicationWhenEndingSession());
        }
        ISessionProvider provider = castContext.getSessionProvider(category);
        if (provider == null) {
            Log.w(TAG, "No session provider for " + category);
            return;
        }
        boolean resume = isCast && TextUtils.equals(routeId, resumeRouteId) && SystemClock.elapsedRealtime() < resumeDeadline;
        String sessionId = resume ? resumeSessionId : null;
        clearPendingResume();
        try {
            SessionImpl session = (SessionImpl) ObjectWrapper.unwrap(provider.getSession(sessionId));
            if (session == null) {
                Log.w(TAG, "Session provider did not create a session");
                return;
            }
            this.currentSession = session;
            if (resume) {
                Log.d(TAG, "Resuming session " + sessionId + " on " + routeId);
                session.resume(castContext, castDevice, routeId, extras);
            } else {
                Log.d(TAG, "Starting session on " + routeId);
                session.start(castContext, castDevice, routeId, extras);
            }
        } catch (RemoteException e) {
            Log.w(TAG, "Error starting session: " + e.getMessage());
        }
    }

    /**
     * Called when a media route got unselected, e.g. because the user tapped "Stop casting" or selected another route.
     */
    public void onRouteUnselected(String routeId, int reason) {
        if (currentSession == null || !TextUtils.equals(currentSession.getRouteId(), routeId)) return;
        if (currentSession.isDisconnecting() || currentSession.isDisconnected()) return;
        boolean stopCasting = reason == MediaRouter.UNSELECT_REASON_STOPPED ||
                (reason != MediaRouter.UNSELECT_REASON_DISCONNECTED && castContext.getOptions().getStopReceiverApplicationWhenEndingSession());
        endCurrentSessionInternal(stopCasting);
    }

    public void onRouteChanged(String routeId, Bundle extras) {
        if (currentSession != null && TextUtils.equals(currentSession.getRouteId(), routeId)) {
            currentSession.onRouteInfoUpdated(extras);
        }
    }

    public void onRouteAdded(String routeId) {
        if (currentSession == null && resumeRouteId != null && TextUtils.equals(routeId, resumeRouteId)) {
            if (SystemClock.elapsedRealtime() >= resumeDeadline) {
                clearPendingResume();
                return;
            }
            Log.d(TAG, "Previous session route " + routeId + " is available again, selecting it");
            try {
                castContext.getRouter().selectRouteById(routeId);
            } catch (RemoteException e) {
                Log.w(TAG, "Error selecting route " + routeId + ": " + e.getMessage());
            }
        }
    }

    private SharedPreferences getPreferences() {
        return castContext.getContext().getApplicationContext().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Remembers the last session of a recoverable session provider, so it can be resumed when the app is restarted.
     */
    void tryResumeSavedSession() {
        SharedPreferences preferences = getPreferences();
        String routeId = preferences.getString(PREF_ROUTE_ID, null);
        String sessionId = preferences.getString(PREF_SESSION_ID, null);
        String category = preferences.getString(PREF_CATEGORY, null);
        if (routeId == null || sessionId == null) return;
        ISessionProvider provider = castContext.defaultSessionProvider;
        try {
            if (provider == null || !TextUtils.equals(category, castContext.getDefaultCategory()) || !provider.isSessionRecoverable()) {
                clearSavedSession();
                return;
            }
        } catch (RemoteException e) {
            return;
        }
        Log.d(TAG, "Trying to resume session " + sessionId + " on " + routeId);
        resumeRouteId = routeId;
        resumeSessionId = sessionId;
        resumeDeadline = SystemClock.elapsedRealtime() + RESUME_TIMEOUT_MS;
        try {
            if (castContext.getRouter().getRouteInfoExtrasById(routeId) != null) {
                onRouteAdded(routeId);
            }
        } catch (RemoteException e) {
            Log.w(TAG, "Error checking route " + routeId + ": " + e.getMessage());
        }
    }

    private void clearPendingResume() {
        resumeRouteId = null;
        resumeSessionId = null;
        resumeDeadline = 0;
    }

    private void saveSession(SessionImpl session) {
        // Only sessions of the default (Cast) session provider are resumed.
        if (!TextUtils.equals(session.getCategory(), castContext.getDefaultCategory())) return;
        if (session.getRouteId() == null || session.getSessionId() == null) return;
        getPreferences().edit()
                .putString(PREF_ROUTE_ID, session.getRouteId())
                .putString(PREF_SESSION_ID, session.getSessionId())
                .putString(PREF_CATEGORY, session.getCategory())
                .apply();
    }

    private void clearSavedSession() {
        getPreferences().edit().clear().apply();
    }

    /**
     * Recalculates the cast state from the session state and the availability of matching routes.
     */
    public void updateCastState() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            // MediaRouter must only be used from the main thread
            castContext.runOnMainThread(this::updateCastState);
            return;
        }
        int newState;
        if (currentSession != null && currentSession.isConnected()) {
            newState = CastState.CONNECTED;
        } else if (currentSession != null && (currentSession.isConnecting() || currentSession.isResuming() || currentSession.isSuspended())) {
            newState = CastState.CONNECTING;
        } else if (isRouteAvailable()) {
            newState = CastState.NOT_CONNECTED;
        } else {
            newState = CastState.NO_DEVICES_AVAILABLE;
        }
        if (newState != castState) {
            Log.d(TAG, "Cast state changed to " + CastState.toString(newState));
            this.castState = newState;
            this.onCastStateChanged();
        }
    }

    private boolean isRouteAvailable() {
        if (castContext.getMergedSelector().isEmpty()) return false;
        try {
            return castContext.getRouter().isRouteAvailable(castContext.getMergedSelector().asBundle(), MediaRouter.AVAILABILITY_FLAG_IGNORE_DEFAULT_ROUTE);
        } catch (RemoteException e) {
            return false;
        }
    }

    public void onCastStateChanged() {
        for (ICastStateListener listener : new ArrayList<>(this.castStateListeners.values())) {
            try {
                listener.onCastStateChanged(this.castState);
            } catch (RemoteException e) {
                Log.d(TAG, "Remote exception calling onCastStateChanged: " + e.getMessage());
            }
        }
    }

    public void onSessionStarting(SessionImpl session) {
        this.updateCastState();
        for (ISessionManagerListener listener : new ArrayList<>(this.sessionManagerListeners.values())) {
            try {
                listener.onSessionStarting(session.getWrappedSession());
            } catch (RemoteException e) {
                Log.d(TAG, "Remote exception calling onSessionStarting: " + e.getMessage());
            }
        }
    }

    public void onSessionStartFailed(SessionImpl session, int error) {
        if (this.currentSession == session) this.currentSession = null;
        this.updateCastState();
        for (ISessionManagerListener listener : new ArrayList<>(this.sessionManagerListeners.values())) {
            try {
                listener.onSessionStartFailed(session.getWrappedSession(), error);
            } catch (RemoteException e) {
                Log.d(TAG, "Remote exception calling onSessionStartFailed: " + e.getMessage());
            }
        }
    }

    public void onSessionStarted(SessionImpl session, String sessionId) {
        this.currentSession = session;
        saveSession(session);
        this.updateCastState();
        for (ISessionManagerListener listener : new ArrayList<>(this.sessionManagerListeners.values())) {
            try {
                listener.onSessionStarted(session.getWrappedSession(), sessionId);
            } catch (RemoteException e) {
                Log.d(TAG, "Remote exception calling onSessionStarted: " + e.getMessage());
            }
        }
    }

    public void onSessionResumed(SessionImpl session, boolean wasSuspended) {
        this.currentSession = session;
        saveSession(session);
        this.updateCastState();
        for (ISessionManagerListener listener : new ArrayList<>(this.sessionManagerListeners.values())) {
            try {
                listener.onSessionResumed(session.getWrappedSession(), wasSuspended);
            } catch (RemoteException e) {
                Log.d(TAG, "Remote exception calling onSessionResumed: " + e.getMessage());
            }
        }
    }

    public void onSessionEnding(SessionImpl session) {
        for (ISessionManagerListener listener : new ArrayList<>(this.sessionManagerListeners.values())) {
            try {
                listener.onSessionEnding(session.getWrappedSession());
            } catch (RemoteException e) {
                Log.d(TAG, "Remote exception calling onSessionEnding: " + e.getMessage());
            }
        }
    }

    public void onSessionEnded(SessionImpl session, int error) {
        if (this.currentSession == session) this.currentSession = null;
        clearSavedSession();
        this.updateCastState();
        for (ISessionManagerListener listener : new ArrayList<>(this.sessionManagerListeners.values())) {
            try {
                listener.onSessionEnded(session.getWrappedSession(), error);
            } catch (RemoteException e) {
                Log.d(TAG, "Remote exception calling onSessionEnded: " + e.getMessage());
            }
        }
    }

    public void onSessionResuming(SessionImpl session, String sessionId) {
        this.updateCastState();
        for (ISessionManagerListener listener : new ArrayList<>(this.sessionManagerListeners.values())) {
            try {
                listener.onSessionResuming(session.getWrappedSession(), sessionId);
            } catch (RemoteException e) {
                Log.d(TAG, "Remote exception calling onSessionResuming: " + e.getMessage());
            }
        }
    }

    public void onSessionResumeFailed(SessionImpl session, int error) {
        if (this.currentSession == session) this.currentSession = null;
        clearSavedSession();
        this.updateCastState();
        for (ISessionManagerListener listener : new ArrayList<>(this.sessionManagerListeners.values())) {
            try {
                listener.onSessionResumeFailed(session.getWrappedSession(), error);
            } catch (RemoteException e) {
                Log.d(TAG, "Remote exception calling onSessionResumeFailed: " + e.getMessage());
            }
        }
    }

    public void onSessionSuspended(SessionImpl session, int reason) {
        this.updateCastState();
        for (ISessionManagerListener listener : new ArrayList<>(this.sessionManagerListeners.values())) {
            try {
                listener.onSessionSuspended(session.getWrappedSession(), reason);
            } catch (RemoteException e) {
                Log.d(TAG, "Remote exception calling onSessionSuspended: " + e.getMessage());
            }
        }
    }
}
