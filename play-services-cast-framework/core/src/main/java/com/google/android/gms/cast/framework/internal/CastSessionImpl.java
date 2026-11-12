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

import com.google.android.gms.cast.framework.ICastSession;

import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.ICastConnectionController;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;

public class CastSessionImpl extends ICastSession.Stub {
    private static final String TAG = CastSessionImpl.class.getSimpleName();
    private CastOptions options;
    private SessionImpl session;
    private ICastConnectionController controller;
    private String sessionId;
    private boolean disconnected;
    private boolean stopCasting;

    public CastSessionImpl(CastOptions options, IObjectWrapper session, ICastConnectionController controller) throws RemoteException {
        this.options = options;
        this.session = (SessionImpl) ObjectWrapper.unwrap(session);
        this.controller = controller;

        this.session.setCastSession(this);
    }

    public void launchApplication() throws RemoteException {
        this.controller.launchApplication(this.options.getReceiverApplicationId(), this.options.getLaunchOptions());
    }

    @Override
    public void onConnected(Bundle routeInfoExtra) throws RemoteException {
        this.controller.launchApplication(this.options.getReceiverApplicationId(), this.options.getLaunchOptions());
    }

    @Override
    public void onConnectionSuspended(int reason) {
        Log.d(TAG, "unimplemented Method: onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(Status status) {
        Log.d(TAG, "unimplemented Method: onConnectionFailed");
    }

    @Override
    public void onApplicationConnectionSuccess(ApplicationMetadata applicationMetadata, String applicationStatus, String sessionId, boolean wasLaunched) {
        if (this.disconnected) {
            if (this.stopCasting) {
                try {
                    this.controller.stopApplication(sessionId);
                } catch (RemoteException ex) {
                    Log.e(TAG, "Error calling stopApplication: " + ex.getMessage());
                }
            }
            return;
        }
        this.sessionId = sessionId;
        this.session.onApplicationConnectionSuccess(applicationMetadata, applicationStatus, sessionId, wasLaunched);
    }

    @Override
    public void onApplicationConnectionFailure(int statusCode) {
        if (this.disconnected) {
            return;
        }
        this.session.onApplicationConnectionFailure(statusCode);
    }

    @Override
    public void disconnectFromDevice(boolean stopCasting, int reason) throws RemoteException {
        this.disconnected = true;
        this.stopCasting = stopCasting;
        if (stopCasting && this.sessionId != null) {
            try {
                this.controller.stopApplication(this.sessionId);
            } catch (RemoteException ex) {
                Log.e(TAG, "Error calling stopApplication: " + ex.getMessage());
            }
        }
        this.sessionId = null;
        this.controller.closeConnection(reason);
    }
}
