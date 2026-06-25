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

package org.microg.gms.cast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.ApplicationStatus;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastDeviceStatus;
import com.google.android.gms.cast.JoinOptions;
import com.google.android.gms.cast.LaunchOptions;
import com.google.android.gms.cast.internal.ICastDeviceController;
import com.google.android.gms.cast.internal.ICastDeviceControllerListener;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.images.WebImage;
import com.google.android.gms.common.internal.BinderWrapper;

import su.litvak.chromecast.api.v2.Application;
import su.litvak.chromecast.api.v2.ChromeCast;
import su.litvak.chromecast.api.v2.ChromeCastConnectionEvent;
import su.litvak.chromecast.api.v2.ChromeCastConnectionEventListener;
import su.litvak.chromecast.api.v2.ChromeCastRawMessage;
import su.litvak.chromecast.api.v2.ChromeCastRawMessageListener;
import su.litvak.chromecast.api.v2.ChromeCastSpontaneousEvent;
import su.litvak.chromecast.api.v2.ChromeCastSpontaneousEventListener;
import su.litvak.chromecast.api.v2.Namespace;

public class CastDeviceControllerImpl extends ICastDeviceController.Stub implements
        ChromeCastConnectionEventListener,
        ChromeCastSpontaneousEventListener,
        ChromeCastRawMessageListener,
        ICastDeviceControllerListener {

    private static final String TAG = "GmsCastDeviceController";

    private final Context context;
    private final String packageName;
    private final CastDevice castDevice;
    final boolean notificationEnabled;
    final long castFlags;

    ICastDeviceControllerListener listener;

    final ChromeCast chromecast;

    String sessionId = null;

    // Serialize all network operations to avoid race conditions on the ChromeCast connection.
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public CastDeviceControllerImpl(Context context, String packageName, Bundle extras) {
        this.context = context;
        this.packageName = packageName;

        extras.setClassLoader(BinderWrapper.class.getClassLoader());
        this.castDevice = CastDevice.getFromBundle(extras);
        this.notificationEnabled = extras.getBoolean(
                "com.google.android.gms.cast.EXTRA_CAST_FRAMEWORK_NOTIFICATION_ENABLED");
        this.castFlags = extras.getLong("com.google.android.gms.cast.EXTRA_CAST_FLAGS");

        BinderWrapper listenerWrapper = (BinderWrapper) extras.get("listener");
        if (listenerWrapper != null) {
            this.listener = ICastDeviceControllerListener.Stub.asInterface(listenerWrapper.binder);
        }

        this.chromecast = new ChromeCast(this.castDevice.getAddress());
        this.chromecast.registerListener(this);
        this.chromecast.registerRawMessageListener(this);
        this.chromecast.registerConnectionListener(this);
    }

    // ---- ICastDeviceController ----

    /**
     * Establishes a TCP/TLS connection to the Chromecast device and fires onConnected when done.
     * Must be called before launchApplication or joinApplication if the caller manages the
     * lifecycle explicitly. Both launch/join will also connect lazily if needed.
     */
    @Override
    public void connect() {
        executor.execute(() -> {
            try {
                if (!chromecast.isConnected()) {
                    chromecast.connect();
                }
                onConnectedWithResult(0);
            } catch (IOException | java.security.GeneralSecurityException e) {
                Log.e(TAG, "Error connecting to Chromecast: " + e.getMessage());
                onApplicationConnectionFailure(CommonStatusCodes.NETWORK_ERROR);
            }
        });
    }

    /**
     * Registers a listener post-construction. Needed when the listener binder is not available
     * in the initial Bundle extras (e.g. certain SDK client paths).
     */
    @Override
    public void addListener(IBinder binder) {
        if (binder != null) {
            this.listener = ICastDeviceControllerListener.Stub.asInterface(binder);
        }
    }

    @Override
    public void disconnect() {
        executor.execute(() -> {
            try {
                chromecast.disconnect();
            } catch (IOException | java.security.GeneralSecurityException e) {
                Log.e(TAG, "Error disconnecting: " + e.getMessage());
            }
            // onDisconnected is fired via connectionEventReceived when the socket closes.
        });
    }

    @Override
    public void launchApplication(String applicationId, LaunchOptions launchOptions) {
        executor.execute(() -> {
            try {
                if (!chromecast.isConnected()) {
                    chromecast.connect();
                }
                Application app = chromecast.launchApp(applicationId);
                this.sessionId = app.sessionId;
                ApplicationMetadata metadata = createMetadataFromApplication(app);
                onApplicationConnectionSuccess(metadata, app.statusText, app.sessionId, true);
            } catch (IOException | java.security.GeneralSecurityException e) {
                Log.w(TAG, "Error launching application: " + e.getMessage());
                onApplicationConnectionFailure(CommonStatusCodes.NETWORK_ERROR);
            }
        });
    }

    /**
     * Joins an existing receiver session if one matching applicationId/sessionId is active.
     * Falls back to launching a fresh session if the app is not currently running or the
     * session ID does not match.
     */
    @Override
    public void joinApplication(String applicationId, String sessionId, JoinOptions joinOptions) {
        executor.execute(() -> {
            try {
                if (!chromecast.isConnected()) {
                    chromecast.connect();
                }

                su.litvak.chromecast.api.v2.Status status = chromecast.getStatus();
                Application runningApp = (status != null) ? status.getRunningApp() : null;

                boolean canJoin = runningApp != null
                        && runningApp.id.equals(applicationId)
                        && (sessionId == null || runningApp.sessionId.equals(sessionId));

                if (canJoin) {
                    // The app is already running — join without relaunching (wasLaunched=false).
                    this.sessionId = runningApp.sessionId;
                    ApplicationMetadata metadata = createMetadataFromApplication(runningApp);
                    onApplicationConnectionSuccess(
                            metadata, runningApp.statusText, runningApp.sessionId, false);
                } else {
                    // App not running or session mismatch — launch fresh.
                    Application app = chromecast.launchApp(applicationId);
                    this.sessionId = app.sessionId;
                    ApplicationMetadata metadata = createMetadataFromApplication(app);
                    onApplicationConnectionSuccess(metadata, app.statusText, app.sessionId, true);
                }
            } catch (IOException | java.security.GeneralSecurityException e) {
                Log.w(TAG, "Error joining application: " + e.getMessage());
                onApplicationConnectionFailure(CommonStatusCodes.NETWORK_ERROR);
            }
        });
    }

    @Override
    public void stopApplication(String sessionId) {
        executor.execute(() -> {
            try {
                chromecast.stopSession(sessionId);
            } catch (IOException | java.security.GeneralSecurityException e) {
                Log.w(TAG, "Error stopping session: " + e.getMessage());
            }
            this.sessionId = null;
        });
    }

    @Override
    public void sendMessage(String namespace, String message, long requestId) {
        executor.execute(() -> {
            try {
                chromecast.sendRawRequest(namespace, message, requestId);
            } catch (IOException | java.security.GeneralSecurityException e) {
                Log.w(TAG, "Error sending cast message: " + e.getMessage());
                onSendMessageFailure("", requestId, CommonStatusCodes.NETWORK_ERROR);
            }
        });
    }

    @Override
    public void registerNamespace(String namespace) {
        // Namespace filtering is not needed: all incoming messages are forwarded via
        // rawMessageReceived regardless of namespace.
        Log.d(TAG, "registerNamespace: " + namespace);
    }

    @Override
    public void unregisterNamespace(String namespace) {
        Log.d(TAG, "unregisterNamespace: " + namespace);
    }

    // ---- ChromeCastConnectionEventListener ----

    @Override
    public void connectionEventReceived(ChromeCastConnectionEvent event) {
        if (!event.isConnected()) {
            onDisconnected(CommonStatusCodes.SUCCESS);
        }
    }

    // ---- ChromeCastSpontaneousEventListener ----

    @Override
    public void spontaneousEventReceived(ChromeCastSpontaneousEvent event) {
        switch (event.getType()) {
            case STATUS: {
                su.litvak.chromecast.api.v2.Status status =
                        (su.litvak.chromecast.api.v2.Status) event.getData();
                Application app = status.getRunningApp();
                ApplicationMetadata metadata = createMetadataFromApplication(app);
                if (app != null) {
                    onApplicationStatusChanged(new ApplicationStatus(app.statusText));
                }
                int activeInput = status.activeInput ? 1 : 0;
                int standby = status.standBy ? 1 : 0;
                onDeviceStatusChanged(new CastDeviceStatus(
                        status.volume.level, status.volume.muted, activeInput, metadata, standby));
                break;
            }
            case CLOSE:
                onApplicationDisconnected(CommonStatusCodes.SUCCESS);
                break;
            default:
                break;
        }
    }

    // ---- ChromeCastRawMessageListener ----

    @Override
    public void rawMessageReceived(ChromeCastRawMessage message, Long requestId) {
        switch (message.getPayloadType()) {
            case STRING:
                String payload = message.getPayloadUtf8();
                if (requestId == null) {
                    onTextMessageReceived(message.getNamespace(), payload);
                } else {
                    onSendMessageSuccess(payload, requestId);
                    onTextMessageReceived(message.getNamespace(), payload);
                }
                break;
            case BINARY:
                onBinaryMessageReceived(message.getNamespace(), message.getPayloadBinary());
                break;
        }
    }

    // ---- Helpers ----

    private ApplicationMetadata createMetadataFromApplication(Application app) {
        if (app == null) return null;
        ApplicationMetadata metadata = new ApplicationMetadata();
        metadata.applicationId = app.id;
        metadata.name = app.name;
        metadata.images = new ArrayList<WebImage>();
        metadata.namespaces = new ArrayList<String>();
        for (Namespace ns : app.namespaces) {
            metadata.namespaces.add(ns.name);
        }
        metadata.senderAppIdentifier = context.getPackageName();
        return metadata;
    }

    // ---- Listener dispatch ----

    public void onConnectedWithResult(int statusCode) {
        if (listener != null) {
            try {
                listener.onConnectedWithResult(statusCode);
            } catch (RemoteException ex) {
                Log.e(TAG, "Error calling onConnected: " + ex.getMessage());
            }
        }
    }

    public void onDisconnected(int reason) {
        if (listener != null) {
            try {
                listener.onDisconnected(reason);
            } catch (RemoteException ex) {
                Log.e(TAG, "Error calling onDisconnected: " + ex.getMessage());
            }
        }
    }

    public void onApplicationConnectionSuccess(ApplicationMetadata metadata, String appStatus,
            String sessionId, boolean wasLaunched) {
        if (listener != null) {
            try {
                listener.onApplicationConnectionSuccess(metadata, appStatus, sessionId, wasLaunched);
            } catch (RemoteException ex) {
                Log.e(TAG, "Error calling onApplicationConnectionSuccess: " + ex.getMessage());
            }
        }
    }

    public void onApplicationConnectionFailure(int statusCode) {
        if (listener != null) {
            try {
                listener.onApplicationConnectionFailure(statusCode);
            } catch (RemoteException ex) {
                Log.e(TAG, "Error calling onApplicationConnectionFailure: " + ex.getMessage());
            }
        }
    }

    public void onApplicationDisconnected(int code) {
        if (listener != null) {
            try {
                listener.onApplicationDisconnected(code);
            } catch (RemoteException ex) {
                Log.e(TAG, "Error calling onApplicationDisconnected: " + ex.getMessage());
            }
        }
    }

    public void onTextMessageReceived(String namespace, String message) {
        if (listener != null) {
            try {
                listener.onTextMessageReceived(namespace, message);
            } catch (RemoteException ex) {
                Log.e(TAG, "Error calling onTextMessageReceived: " + ex.getMessage());
            }
        }
    }

    public void onBinaryMessageReceived(String namespace, byte[] data) {
        if (listener != null) {
            try {
                listener.onBinaryMessageReceived(namespace, data);
            } catch (RemoteException ex) {
                Log.e(TAG, "Error calling onBinaryMessageReceived: " + ex.getMessage());
            }
        }
    }

    public void onSendMessageSuccess(String response, long requestId) {
        if (listener != null) {
            try {
                listener.onSendMessageSuccess(response, requestId);
            } catch (RemoteException ex) {
                Log.e(TAG, "Error calling onSendMessageSuccess: " + ex.getMessage());
            }
        }
    }

    public void onSendMessageFailure(String response, long requestId, int statusCode) {
        if (listener != null) {
            try {
                listener.onSendMessageFailure(response, requestId, statusCode);
            } catch (RemoteException ex) {
                Log.e(TAG, "Error calling onSendMessageFailure: " + ex.getMessage());
            }
        }
    }

    public void onApplicationStatusChanged(ApplicationStatus status) {
        if (listener != null) {
            try {
                listener.onApplicationStatusChanged(status);
            } catch (RemoteException ex) {
                Log.e(TAG, "Error calling onApplicationStatusChanged: " + ex.getMessage());
            }
        }
    }

    public void onDeviceStatusChanged(CastDeviceStatus status) {
        if (listener != null) {
            try {
                listener.onDeviceStatusChanged(status);
            } catch (RemoteException ex) {
                Log.e(TAG, "Error calling onDeviceStatusChanged: " + ex.getMessage());
            }
        }
    }
}
