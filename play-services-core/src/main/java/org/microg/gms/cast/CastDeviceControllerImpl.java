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

package org.microg.gms.cast;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.JoinOptions;
import com.google.android.gms.cast.LaunchOptions;
import com.google.android.gms.cast.internal.ICastDeviceController;
import com.google.android.gms.cast.internal.ICastDeviceControllerListener;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CastDeviceControllerImpl extends ICastDeviceController.Stub {
    private static final String TAG = "CastDeviceController";
    private final Context context;
    private final CastDevice castDevice;
    private final CopyOnWriteArrayList<ICastDeviceControllerListener> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private boolean connected = false;
    private String currentSessionId;

    public CastDeviceControllerImpl(Context context, CastDevice castDevice) {
        this.context = context;
        this.castDevice = castDevice;
    }

    @Override
    public void addListener(ICastDeviceControllerListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void connect() {
        networkExecutor.execute(() -> {
            try {
                Log.d(TAG, "Connecting to Cast Device: " + castDevice.getInetAddress() + ":" + castDevice.getServicePort());
                connected = true;
                currentSessionId = "session-" + System.currentTimeMillis();
                for (ICastDeviceControllerListener listener : listeners) {
                    try {
                        listener.onConnected(currentSessionId);
                    } catch (RemoteException e) {
                        Log.w(TAG, "Listener error", e);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Connection failed", e);
                for (ICastDeviceControllerListener listener : listeners) {
                    try {
                        listener.onApplicationConnectionFailed(2005);
                    } catch (RemoteException ignored) {}
                }
            }
        });
    }

    @Override
    public void disconnect() {
        networkExecutor.execute(() -> {
            connected = false;
            for (ICastDeviceControllerListener listener : listeners) {
                try {
                    listener.onApplicationDisconnected(0);
                } catch (RemoteException ignored) {}
            }
        });
    }

    @Override
    public void launchApplication(String applicationId, LaunchOptions launchOptions) {
        networkExecutor.execute(() -> {
            Log.d(TAG, "launchApplication: " + applicationId);
        });
    }

    @Override
    public void joinApplication(String applicationId, String sessionId, JoinOptions joinOptions) {
        networkExecutor.execute(() -> {
            Log.d(TAG, "joinApplication: " + applicationId + " session: " + sessionId);
        });
    }

    @Override
    public void stopApplication(String sessionId) {
        networkExecutor.execute(() -> {
            Log.d(TAG, "stopApplication: " + sessionId);
        });
    }

    @Override
    public void sendMessage(String namespace, String message, long requestId) {
        networkExecutor.execute(() -> {
            Log.d(TAG, "sendMessage [" + namespace + "]: " + message);
        });
    }

    @Override
    public void setVolume(double volume, double oldVolume, boolean isMute) {
        networkExecutor.execute(() -> {
            Log.d(TAG, "setVolume: " + volume + ", isMute: " + isMute);
        });
    }

    @Override
    public void setMute(boolean isMute, double oldVolume, boolean wasMute) {
        networkExecutor.execute(() -> {
            Log.d(TAG, "setMute: " + isMute);
        });
    }

    @Override
    public void requestStatus() {
        networkExecutor.execute(() -> {
            Log.d(TAG, "requestStatus");
        });
    }
}