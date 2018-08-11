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
import java.util.Collections;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Base64;
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
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.images.WebImage;
import com.google.android.gms.common.internal.BinderWrapper;
import com.google.android.gms.common.internal.GetServiceRequest;

import su.litvak.chromecast.api.v2.Application;
import su.litvak.chromecast.api.v2.ChromeCast;
import su.litvak.chromecast.api.v2.Namespace;
import su.litvak.chromecast.api.v2.ChromeCastSpontaneousEventListener;
import su.litvak.chromecast.api.v2.ChromeCastRawMessageListener;
import su.litvak.chromecast.api.v2.ChromeCastSpontaneousEvent;
import su.litvak.chromecast.api.v2.ChromeCastRawMessage;
import su.litvak.chromecast.api.v2.AppEvent;

public class CastDeviceControllerImpl extends ICastDeviceController.Stub
    implements ChromeCastSpontaneousEventListener, ChromeCastRawMessageListener, ICastDeviceControllerListener
{
    private static final String TAG = "GmsCastDeviceControllerImpl";

    private Context context;
    private String packageName;
    private CastDevice castDevice;
    boolean notificationEnabled;
    long castFlags;
    ICastDeviceControllerListener listener;

    ChromeCast chromecast;

    String sessionId = null;

    public CastDeviceControllerImpl(Context context, String packageName, Bundle extras) {
        this.context = context;
        this.packageName = packageName;

        extras.setClassLoader(BinderWrapper.class.getClassLoader());
        this.castDevice = CastDevice.getFromBundle(extras);
        this.notificationEnabled = extras.getBoolean("com.google.android.gms.cast.EXTRA_CAST_FRAMEWORK_NOTIFICATION_ENABLED");
        this.castFlags = extras.getLong("com.google.android.gms.cast.EXTRA_CAST_FLAGS");
        BinderWrapper listenerWrapper = (BinderWrapper)extras.get("listener");
        if (listenerWrapper != null) {
            this.listener = ICastDeviceControllerListener.Stub.asInterface(listenerWrapper.binder);
        }

        this.chromecast = new ChromeCast(this.castDevice.getAddress());
        this.chromecast.registerListener(this);
        this.chromecast.registerRawMessageListener(this);
    }

    @Override
    public void spontaneousEventReceived(ChromeCastSpontaneousEvent event) {
        switch (event.getType()) {
            case MEDIA_STATUS:
                Log.d(TAG, "unimplemented Method: spontaneousEventReceived: MEDIA_STATUS");
                break;
            case STATUS:
                Log.d(TAG, "unimplemented Method: spontaneousEventReceived: STATUS");
                Application app = ((su.litvak.chromecast.api.v2.Status)event.getData()).getRunningApp();
                if (app != null) {
                    this.onApplicationStatusChanged(new ApplicationStatus(app.statusText));
                }
                break;
            case APPEVENT:
                Log.d(TAG, "unimplemented Method: spontaneousEventReceived: APPEVENT" + ((AppEvent)event.getData()).message);
                break;
            case CLOSE:
                Log.d(TAG, "unimplemented Method: spontaneousEventReceived: CLOSE");
                break;
            default:
                Log.d(TAG, "unimplemented Method: spontaneousEventReceived: UNKNOWN");
                break;
        }
    }

    @Override
    public void rawMessageReceived(ChromeCastRawMessage message, Long requestId) {
        switch (message.getPayloadType()) {
            case STRING:
                String response = message.getPayloadUtf8();
                if (requestId == null) {
                    this.onTextMessageReceived(message.getNamespace(), response);
                } else {
                    this.onSendMessageSuccess(response, requestId);
                }
                break;
            case BINARY:
                Log.d(TAG, "unimplemented Method: rawMessageReceived: BINARY");
                break;
        }
    }

    @Override
    public void disconnect() {
        Log.d(TAG, "unimplemented Method: disconnect");
        this.sessionId = null;
    }

    @Override
    public void sendMessage(String namespace, String message, long requestId) {
        Log.d(TAG, "unimplemented Method: sendMessage" + message);
        try {
            this.chromecast.sendRawRequest(namespace, message, requestId);
        } catch (IOException e) {
            Log.w(TAG, "Error sending cast message: " + e.getMessage());
            return;
        }
    }

    @Override
    public void stopApplication(String sessionId) {
        Log.d(TAG, "unimplemented Method: stopApplication");
        try {
            // TODO: Expose more control of chromecast-java-api-v2 so we can
            // make use of our sessionID parameter.
            this.chromecast.stopApp();
        } catch (IOException e) {
            Log.w(TAG, "Error sending cast message: " + e.getMessage());
            return;
        }
        this.sessionId = null;
    }

    @Override
    public void registerNamespace(String namespace) {
        Log.d(TAG, "unimplemented Method: registerNamespace");
    }

    @Override
    public void unregisterNamespace(String namespace) {
        Log.d(TAG, "unimplemented Method: unregisterNamespace");
    }

    @Override
    public void launchApplication(String applicationId, LaunchOptions launchOptions) {
        Application app = null;
        try {
            app = this.chromecast.launchApp(applicationId);
        } catch (IOException e) {
            Log.w(TAG, "Error launching cast application: " + e.getMessage());
            this.onApplicationConnectionFailure(CommonStatusCodes.NETWORK_ERROR);
            return;
        }
        this.sessionId = app.sessionId;

        ApplicationMetadata metadata = new ApplicationMetadata();
        metadata.applicationId = applicationId;
        metadata.name = app.name;
        Log.d(TAG, "unimplemented: ApplicationMetadata.images");
        metadata.images = new ArrayList<WebImage>();
        metadata.namespaces = new ArrayList<String>();
        Log.d(TAG, "unimplemented: ApplicationMetadata.senderAppLaunchUri");
        for(Namespace namespace : app.namespaces) {
            metadata.namespaces.add(namespace.name);
        }
        metadata.senderAppIdentifier = this.context.getPackageName();
        this.onApplicationConnectionSuccess(metadata, app.statusText, app.sessionId, true);
    }

    @Override
    public void joinApplication(String applicationId, String sessionId, JoinOptions joinOptions) {
        Log.d(TAG, "unimplemented Method: joinApplication");
        this.launchApplication(applicationId, new LaunchOptions());
    }

    public void onDisconnected(int reason) {
        Log.d(TAG, "unimplemented Method: onDisconnected");
        if (this.listener != null) {
            try {
                this.listener.onDisconnected(reason);
            } catch (RemoteException ex) {
                Log.e(TAG, "Error calling onDisconnected: " + ex.getMessage());
            }
        }
    }

    public void onApplicationConnectionSuccess(ApplicationMetadata applicationMetadata, String applicationStatus, String sessionId, boolean wasLaunched) {
        Log.d(TAG, "unimplemented Method: onApplicationConnectionSuccess");
        if (this.listener != null) {
            try {
                this.listener.onApplicationConnectionSuccess(applicationMetadata, applicationStatus, sessionId, wasLaunched);
            } catch (RemoteException ex) {
                Log.e(TAG, "Error calling onApplicationConnectionSuccess: " + ex.getMessage());
            }
        }
    }

    public void onApplicationConnectionFailure(int statusCode) {
        Log.d(TAG, "unimplemented Method: onApplicationConnectionFailure");
        if (this.listener != null) {
            try {
                this.listener.onApplicationConnectionFailure(statusCode);
            } catch (RemoteException ex) {
                Log.e(TAG, "Error calling onApplicationConnectionFailure: " + ex.getMessage());
            }
        }
    }

    public void onTextMessageReceived(String namespace, String message) {
        Log.d(TAG, "unimplemented Method: onTextMessageReceived: " + message);
        if (this.listener != null) {
            try {
                this.listener.onTextMessageReceived(namespace, message);
            } catch (RemoteException ex) {
                Log.e(TAG, "Error calling onTextMessageReceived: " + ex.getMessage());
            }
        }
    }

    public void onBinaryMessageReceived(String namespace, byte[] data) {
        Log.d(TAG, "unimplemented Method: onBinaryMessageReceived");
        if (this.listener != null) {
            try {
                this.listener.onBinaryMessageReceived(namespace, data);
            } catch (RemoteException ex) {
                Log.e(TAG, "Error calling onBinaryMessageReceived: " + ex.getMessage());
            }
        }
    }

    public void onApplicationDisconnected(int paramInt) {
        Log.d(TAG, "unimplemented Method: onApplicationDisconnected");
        if (this.listener != null) {
            try {
                this.listener.onApplicationDisconnected(paramInt);
            } catch (RemoteException ex) {
                Log.e(TAG, "Error calling onApplicationDisconnected: " + ex.getMessage());
            }
        }
    }

    public void onSendMessageFailure(String response, long requestId, int statusCode) {
        Log.d(TAG, "unimplemented Method: onSendMessageFailure");
        if (this.listener != null) {
            try {
                this.listener.onSendMessageFailure(response, requestId, statusCode);
            } catch (RemoteException ex) {
                Log.e(TAG, "Error calling onSendMessageFailure: " + ex.getMessage());
            }
        }
    }

    public void onSendMessageSuccess(String response, long requestId) {
        Log.d(TAG, "unimplemented Method: onSendMessageSuccess: " + response);
        if (this.listener != null) {
            try {
                this.listener.onSendMessageSuccess(response, requestId);
            } catch (RemoteException ex) {
                Log.e(TAG, "Error calling onSendMessageSuccess: " + ex.getMessage());
            }
        }
    }

    public void onApplicationStatusChanged(ApplicationStatus applicationStatus) {
        Log.d(TAG, "unimplemented Method: onApplicationStatusChanged");
        if (this.listener != null) {
            try {
                this.listener.onApplicationStatusChanged(applicationStatus);
            } catch (RemoteException ex) {
                Log.e(TAG, "Error calling onApplicationStatusChanged: " + ex.getMessage());
            }
        }
    }

    public void onDeviceStatusChanged(CastDeviceStatus deviceStatus) {
        Log.d(TAG, "unimplemented Method: onDeviceStatusChanged");
        if (this.listener != null) {
            try {
                this.listener.onDeviceStatusChanged(deviceStatus);
            } catch (RemoteException ex) {
                Log.e(TAG, "Error calling onDeviceStatusChanged: " + ex.getMessage());
            }
        }
    }
}
