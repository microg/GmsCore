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
import com.google.android.gms.cast.LaunchOptions;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.ICastConnectionController;
import com.google.android.gms.cast.framework.ICastSession;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;

/**
 * Module side of {@code CastSession}.
 * <p>
 * The client library owns the actual connection to the Cast device (through the Cast API). It reports connection
 * events here and we tell it what to do next using the {@link ICastConnectionController}:
 * <ol>
 * <li>{@code ISessionProxy.start()} makes the client connect to the device</li>
 * <li>{@link #onConnected(Bundle)} -> we ask to launch (or join) the receiver application</li>
 * <li>{@link #onApplicationConnectionSuccess} -> session is started</li>
 * </ol>
 */
public class CastSessionImpl extends ICastSession.Stub {
    private static final String TAG = "GmsCastSessionImpl";

    private final CastOptions options;
    private final SessionImpl session;
    private final ICastConnectionController controller;

    public CastSessionImpl(CastOptions options, IObjectWrapper session, ICastConnectionController controller) throws RemoteException {
        this.options = options;
        this.session = (SessionImpl) ObjectWrapper.unwrap(session);
        this.controller = controller;

        if (this.session != null) {
            this.session.setCastSession(this);
        }
    }

    private String getReceiverApplicationId() {
        return this.options == null ? null : this.options.getReceiverApplicationId();
    }

    @Override
    public void onConnected(Bundle routeInfoExtra) throws RemoteException {
        String applicationId = getReceiverApplicationId();
        if (this.session != null && (this.session.isDisconnecting() || this.session.isDisconnected())) {
            // The session was ended while the client was still connecting
            Log.d(TAG, "Connected to device after the session ended, closing connection");
            try {
                this.controller.closeConnection(CommonStatusCodes.SUCCESS);
            } catch (Exception e) {
                Log.w(TAG, "Failed to close connection", e);
            }
            return;
        }
        try {
            if (this.session != null && (this.session.isResuming() || this.session.isSuspended())) {
                // Receiver application is expected to be running already, don't restart it
                Log.d(TAG, "Connected to device, joining " + applicationId + " session " + this.session.getSessionId());
                this.controller.joinApplication(applicationId, this.session.getSessionId());
            } else {
                Log.d(TAG, "Connected to device, launching " + applicationId);
                LaunchOptions launchOptions = this.options == null ? null : this.options.getLaunchOptions();
                this.controller.launchApplication(applicationId, launchOptions == null ? new LaunchOptions() : launchOptions);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to request application launch", e);
            onApplicationConnectionFailure(CommonStatusCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public void onConnectionSuspended(int reason) {
        Log.d(TAG, "onConnectionSuspended: " + reason);
        if (this.session != null) this.session.notifySessionSuspended(reason);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        int errorCode = result == null ? CommonStatusCodes.ERROR : result.getErrorCode();
        Log.d(TAG, "onConnectionFailed: " + errorCode);
        // Also invoked when the connection is closed after the session was ended, which SessionImpl ignores
        if (this.session != null) this.session.notifySessionEnded(errorCode);
    }

    @Override
    public void onApplicationConnectionSuccess(ApplicationMetadata applicationMetadata, String applicationStatus, String sessionId, boolean wasLaunched) {
        Log.d(TAG, "onApplicationConnectionSuccess: sessionId=" + sessionId + " wasLaunched=" + wasLaunched);
        if (this.session == null) return;
        if (this.session.isResuming() || this.session.isSuspended()) {
            this.session.setSessionId(sessionId);
            this.session.notifySessionResumed(this.session.isSuspended());
        } else {
            this.session.notifySessionStarted(sessionId);
        }
    }

    @Override
    public void onApplicationConnectionFailure(int statusCode) {
        Log.d(TAG, "onApplicationConnectionFailure: " + statusCode);
        // Report first: Closing the connection makes the client report a connection failure with a less specific code
        if (this.session != null) this.session.notifySessionEnded(statusCode);
        try {
            this.controller.closeConnection(statusCode);
        } catch (Exception e) {
            Log.w(TAG, "Failed to close connection", e);
        }
    }

    @Override
    public void disconnectFromDevice(boolean stopCasting, int reason) {
        Log.d(TAG, "disconnectFromDevice: stopCasting=" + stopCasting + " reason=" + reason);
        try {
            String sessionId = this.session == null ? null : this.session.getSessionId();
            if (stopCasting && sessionId != null) {
                this.controller.stopApplication(sessionId);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to stop application", e);
        }
        try {
            this.controller.closeConnection(reason);
        } catch (Exception e) {
            Log.w(TAG, "Failed to close connection", e);
        }
    }
}
