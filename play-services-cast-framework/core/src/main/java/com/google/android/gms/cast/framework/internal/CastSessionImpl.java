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
import com.google.android.gms.cast.CastStatusCodes;
import com.google.android.gms.cast.LaunchOptions;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.ICastConnectionController;
import com.google.android.gms.cast.framework.ICastSession;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;

/**
 * Module side of the client library CastSession. The client library connects to the Cast device and reports the connection
 * state here; this class decides whether to launch or join the receiver application and translates the results into
 * session state changes.
 */
public class CastSessionImpl extends ICastSession.Stub {
    private static final String TAG = CastSessionImpl.class.getSimpleName();
    private final CastOptions options;
    private final SessionImpl session;
    private final ICastConnectionController controller;
    private String applicationSessionId;

    public CastSessionImpl(CastOptions options, IObjectWrapper session, ICastConnectionController controller) throws RemoteException {
        this.options = options;
        this.session = (SessionImpl) ObjectWrapper.unwrap(session);
        this.controller = controller;

        this.session.setCastSession(this);
    }

    private String getReceiverApplicationId() {
        return options.getReceiverApplicationId();
    }

    @Override
    public void onConnected(Bundle routeInfoExtra) throws RemoteException {
        Log.d(TAG, "onConnected");
        if (session.isResuming() || session.isSuspended()) {
            String sessionId = applicationSessionId != null ? applicationSessionId : session.getSessionId();
            Log.d(TAG, "Joining application " + getReceiverApplicationId() + " with session " + sessionId);
            this.controller.joinApplication(getReceiverApplicationId(), sessionId);
        } else {
            LaunchOptions launchOptions = options.getLaunchOptions();
            if (launchOptions == null) launchOptions = new LaunchOptions();
            Log.d(TAG, "Launching application " + getReceiverApplicationId());
            this.controller.launchApplication(getReceiverApplicationId(), launchOptions);
        }
    }

    @Override
    public void onConnectionSuspended(int reason) {
        Log.d(TAG, "onConnectionSuspended: " + reason);
        session.notifySessionSuspended(reason);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        int errorCode = connectionResult != null ? connectionResult.getErrorCode() : CastStatusCodes.NETWORK_ERROR;
        Log.d(TAG, "onConnectionFailed: " + errorCode);
        onFailure(errorCode);
    }

    @Override
    public void onApplicationConnectionSuccess(ApplicationMetadata applicationMetadata, String applicationStatus, String sessionId, boolean wasLaunched) {
        Log.d(TAG, "onApplicationConnectionSuccess: " + sessionId + " launched=" + wasLaunched);
        this.applicationSessionId = sessionId;
        if (session.isResuming()) {
            session.notifySessionResumed(false);
        } else if (session.isSuspended()) {
            session.notifySessionResumed(true);
        } else {
            session.notifySessionStarted(sessionId);
        }
    }

    @Override
    public void onApplicationConnectionFailure(int statusCode) {
        Log.d(TAG, "onApplicationConnectionFailure: " + statusCode);
        onFailure(statusCode);
    }

    private void onFailure(int statusCode) {
        if (session.isResuming()) {
            session.notifyFailedToResumeSession(statusCode);
        } else if (session.isConnecting()) {
            session.notifyFailedToStartSession(statusCode);
        } else if (session.isConnected() || session.isSuspended()) {
            session.notifySessionEnded(statusCode);
        } else {
            return;
        }
        closeConnection(statusCode);
    }

    private void closeConnection(int reason) {
        try {
            controller.closeConnection(reason);
        } catch (RemoteException e) {
            Log.w(TAG, "Error closing connection: " + e.getMessage());
        }
    }

    @Override
    public void disconnectFromDevice(boolean stopCasting, int reason) {
        Log.d(TAG, "disconnectFromDevice: stopCasting=" + stopCasting + " reason=" + reason);
        if (stopCasting && applicationSessionId != null) {
            try {
                controller.stopApplication(applicationSessionId);
            } catch (RemoteException e) {
                Log.w(TAG, "Error stopping application: " + e.getMessage());
            }
        }
        closeConnection(reason);
    }
}
