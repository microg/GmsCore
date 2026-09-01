/*
 * Copyright (C) 2013-2026 microG Project Team
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

package org.microg.gms.cast.framework;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.framework.ISessionProxy;

public class SessionImpl {
    private static final String TAG = "SessionImpl";
    private final Context context;
    private final ISessionProxy proxy;
    private final CastDevice device;
    private boolean connected = false;

    public SessionImpl(Context context, ISessionProxy proxy, CastDevice device) {
        this.context = context;
        this.proxy = proxy;
        this.device = device;
    }

    public void start(Bundle extras) {
        try {
            Log.d(TAG, "Starting Cast session for device: " + (device != null ? device.getFriendlyName() : "unknown"));
            if (proxy != null) {
                // Must trigger onSessionStarting before establishing channel
                proxy.onSessionStarting();
                connected = true;
                proxy.onSessionStarted(device != null ? device.getDeviceId() : "");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error starting session", e);
            if (proxy != null) {
                try {
                    proxy.onSessionStartFailed(2005);
                } catch (RemoteException ignored) {}
            }
        }
    }

    public void end(boolean stopCasting) {
        if (!connected) return;
        try {
            if (proxy != null) {
                proxy.onSessionEnding();
                connected = false;
                proxy.onSessionEnded(0);
            }
        } catch (RemoteException e) {
            Log.w(TAG, "Error ending session", e);
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public CastDevice getDevice() {
        return device;
    }
}