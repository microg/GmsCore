/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.cast.framework.internal;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.cast.framework.IReconnectionService;
import com.google.android.gms.cast.framework.ISessionManagerListener;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;

import org.microg.gms.common.Constants;

/**
 * Backs the client library's ReconnectionService, which the client starts while a remote media session is active to keep
 * the app process alive. Reconnecting a suspended session is driven by the Cast connection itself (see
 * {@link CastSessionImpl}), so this service only has to stay alive while there is a session to keep.
 */
public class ReconnectionServiceImpl extends IReconnectionService.Stub {
    private static final String TAG = ReconnectionServiceImpl.class.getSimpleName();

    private final Service service;
    private final SessionManagerImpl sessionManager;

    // The client only stops the service itself when a session ends with reason 0, so stop it here once the session is gone.
    private final ISessionManagerListener.Stub sessionListener = new ISessionManagerListener.Stub() {
        @Override
        public IObjectWrapper getWrappedThis() {
            return ObjectWrapper.wrap(this);
        }

        @Override
        public void onSessionStarting(IObjectWrapper session) {
        }

        @Override
        public void onSessionStarted(IObjectWrapper session, String sessionId) {
        }

        @Override
        public void onSessionStartFailed(IObjectWrapper session, int error) {
            stopIfIdle();
        }

        @Override
        public void onSessionEnding(IObjectWrapper session) {
        }

        @Override
        public void onSessionEnded(IObjectWrapper session, int error) {
            stopIfIdle();
        }

        @Override
        public void onSessionResuming(IObjectWrapper session, String sessionId) {
        }

        @Override
        public void onSessionResumed(IObjectWrapper session, boolean wasSuspended) {
        }

        @Override
        public void onSessionResumeFailed(IObjectWrapper session, int error) {
            stopIfIdle();
        }

        @Override
        public void onSessionSuspended(IObjectWrapper session, int reason) {
        }

        @Override
        public int getSupportedVersion() {
            return Constants.GMS_VERSION_CODE;
        }
    };

    public ReconnectionServiceImpl(IObjectWrapper service, IObjectWrapper sessionManager, IObjectWrapper discoveryManager) {
        this.service = (Service) ObjectWrapper.unwrap(service);
        Object unwrappedSessionManager = ObjectWrapper.unwrap(sessionManager);
        this.sessionManager = unwrappedSessionManager instanceof SessionManagerImpl ? (SessionManagerImpl) unwrappedSessionManager : null;
    }

    private boolean hasActiveSession() {
        if (sessionManager == null) return false;
        SessionImpl session = sessionManager.getCurrentSession();
        return session != null && (session.isConnected() || session.isConnecting() || session.isResuming() || session.isSuspended());
    }

    private void stopIfIdle() {
        if (service != null && !hasActiveSession()) {
            Log.d(TAG, "Session gone, stopping");
            service.stopSelf();
        }
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        if (sessionManager != null) {
            try {
                sessionManager.addSessionManagerListener(sessionListener);
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to add session listener: " + e.getMessage());
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!hasActiveSession()) {
            Log.d(TAG, "No active session, stopping");
            if (service != null) service.stopSelf(startId);
            return Service.START_NOT_STICKY;
        }
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (sessionManager != null) {
            try {
                sessionManager.removeSessionManagerListener(sessionListener);
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to remove session listener: " + e.getMessage());
            }
        }
    }
}
