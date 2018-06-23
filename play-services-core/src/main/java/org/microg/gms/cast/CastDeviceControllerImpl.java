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
import com.google.android.gms.cast.CastDevice;
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

public class CastDeviceControllerImpl extends ICastDeviceController.Stub
    implements ChromeCastSpontaneousEventListener, ChromeCastRawMessageListener
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
        this.listener = ICastDeviceControllerListener.Stub.asInterface(listenerWrapper.binder);

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
                break;
            case APPEVENT:
                Log.d(TAG, "unimplemented Method: spontaneousEventReceived: APPEVENT");
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
    public void rawMessageReceived(ChromeCastRawMessage message) {
        switch (message.getPayloadType()) {
            case STRING:
                try {
                    this.listener.onTextMessageReceived(message.getNamespace(), message.getPayloadUtf8());
                } catch (RemoteException ex) {
                    Log.e(TAG, "Error calling onTextMessageReceived: " + ex.getMessage());
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
        String response = null;
        try {
            response = this.chromecast.sendRawRequest(namespace, message, requestId);
        } catch (IOException e) {
            Log.w(TAG, "Error sending cast message: " + e.getMessage());
            return;
        }
        try {
            this.listener.onSendMessageSuccess(response, requestId);
        } catch (RemoteException ex) {
            Log.e(TAG, "Error calling onSendMessageSuccess: " + ex.getMessage());
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
            try {
                this.listener.onApplicationConnectionFailure(CommonStatusCodes.NETWORK_ERROR);
            } catch (RemoteException ex) {
                Log.e(TAG, "Error calling onApplicationConnectionFailure: " + ex.getMessage());
            }
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
        try {
            this.listener.onApplicationConnectionSuccess(metadata, app.statusText, app.sessionId, true);
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling onApplicationConnectionSuccess: " + e.getMessage());
        }
    }

    @Override
    public void joinApplication(String applicationId, String sessionId, JoinOptions joinOptions) {
        Log.d(TAG, "unimplemented Method: joinApplication");
    }
}
