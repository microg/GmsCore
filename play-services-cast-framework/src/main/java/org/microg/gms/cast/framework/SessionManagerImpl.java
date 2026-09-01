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
import com.google.android.gms.cast.framework.ISessionManagerListener;
import java.util.concurrent.CopyOnWriteArrayList;

public class SessionManagerImpl extends ISessionManager.Stub {
    private static final String TAG = "SessionManagerImpl";
    private final Context context;
    private final CopyOnWriteArrayList<ISessionManagerListener> listeners = new CopyOnWriteArrayList<>();
    private SessionImpl currentSession;
    private boolean deviceAvailable = false;

    public SessionManagerImpl(Context context) {
        this.context = context;
    }

    public void addSessionManagerListener(ISessionManagerListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeSessionManagerListener(ISessionManagerListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    @Override
    public void onRouteSelected(Bundle extras) {
        if (extras == null) return;
        CastDevice device = extras.getParcelable("com.google.android.gms.cast.EXTRA_CAST_DEVICE");
        if (device == null) return;

        Log.d(TAG, "Selected Cast route for: " + device.getFriendlyName());
        if (currentSession != null) {
            currentSession.end(true);
        }
        currentSession = new SessionImpl(context, null, device);
        currentSession.start(extras);
    }

    @Override
    public void onRouteUnselected() {
        Log.d(TAG, "Unselected Cast route");
        if (currentSession != null) {
            currentSession.end(true);
            currentSession = null;
        }
    }

    @Override
    public void onDeviceAvailabilityChanged(boolean available) {
        this.deviceAvailable = available;
        Log.d(TAG, "Device availability changed: " + available);
    }

    public boolean isDeviceAvailable() {
        return deviceAvailable;
    }

    public SessionImpl getCurrentSession() {
        return currentSession;
    }
}