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

package com.google.android.gms.wearable;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.wearable.internal.AmsEntityUpdateParcelable;
import com.google.android.gms.wearable.internal.AncsNotificationParcelable;
import com.google.android.gms.wearable.internal.CapabilityInfoParcelable;
import com.google.android.gms.wearable.internal.ChannelEventParcelable;
import com.google.android.gms.wearable.internal.IWearableListener;
import com.google.android.gms.wearable.internal.MessageEventParcelable;
import com.google.android.gms.wearable.internal.NodeParcelable;

import org.microg.gms.common.PublicApi;
import org.microg.gms.wearable.ChannelImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.microg.gms.common.Constants.GMS_PACKAGE_NAME;

@PublicApi
public abstract class WearableListenerService extends Service implements CapabilityApi.CapabilityListener, ChannelApi.ChannelListener, DataApi.DataListener, MessageApi.MessageListener, NodeApi.NodeListener {
    private static final String BIND_LISTENER_INTENT_ACTION = "com.google.android.gms.wearable.BIND_LISTENER";
    private static final String TAG = "GmsWearListenerSvc";

    private HandlerThread handlerThread;
    private IWearableListener listener;
    private ServiceHandler serviceHandler;
    private Object lock = new Object();
    private boolean disconnected = false;

    @Override
    public IBinder onBind(Intent intent) {
        if (BIND_LISTENER_INTENT_ACTION.equals(intent.getAction())) {
            return listener.asBinder();
        }
        return null;
    }

    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
    }

    public void onConnectedNodes(List<Node> connectedNodes) {
    }

    @Override
    public void onChannelClosed(Channel channel, int closeReason, int appSpecificErrorCode) {
    }

    @Override
    public void onChannelOpened(Channel channel) {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handlerThread = new HandlerThread("WearableListenerService");
        handlerThread.start();
        serviceHandler = new ServiceHandler(handlerThread.getLooper());
        listener = new Listener();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
    }

    @Override
    public void onDestroy() {
        synchronized (lock) {
            if (serviceHandler == null) {
                throw new IllegalStateException("serviceHandler not set, did you override onCreate() but forget to call super.onCreate()?");
            }
            serviceHandler.getLooper().quit();
        }
        super.onDestroy();
    }

    @PublicApi(exclude = true)
    public void onEntityUpdate(AmsEntityUpdate entityUpdate) {
    }

    @Override
    public void onInputClosed(Channel channel, @ChannelApi.CloseReason int closeReason, int appSpecificErrorCode) {
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
    }

    @PublicApi(exclude = true)
    public void onNotificationReceived(AncsNotification notification) {
    }

    @Override
    public void onOutputClosed(Channel channel, @ChannelApi.CloseReason int closeReason, int appSpecificErrorCode) {
    }

    @Override
    public void onPeerConnected(Node peer) {
    }

    @Override
    public void onPeerDisconnected(Node peer) {
    }

    private class Listener extends IWearableListener.Stub {
        private int knownGoodUid = -1;

        private boolean post(Runnable runnable) {
            int callingUid = Binder.getCallingUid();
            if (callingUid != knownGoodUid) {
                // TODO: Verify Gms is calling
                String[] packagesForUid = getPackageManager().getPackagesForUid(callingUid);
                if (packagesForUid != null) {
                    if (Arrays.asList(packagesForUid).contains(GMS_PACKAGE_NAME)) {
                        knownGoodUid = callingUid;
                    } else {
                        throw new SecurityException("Caller is not Services Core");
                    }
                }
            }
            synchronized (lock) {
                if (disconnected) {
                    return false;
                }
                serviceHandler.post(runnable);
                return true;
            }
        }

        @Override
        public void onDataChanged(final DataHolder data) throws RemoteException {
            post(new Runnable() {
                @Override
                public void run() {
                    WearableListenerService.this.onDataChanged(new DataEventBuffer(data));
                }
            });
        }

        @Override
        public void onMessageReceived(final MessageEventParcelable messageEvent) throws RemoteException {
            post(new Runnable() {
                @Override
                public void run() {
                    WearableListenerService.this.onMessageReceived(messageEvent);
                }
            });
        }

        @Override
        public void onPeerConnected(final NodeParcelable node) throws RemoteException {
            post(new Runnable() {
                @Override
                public void run() {
                    WearableListenerService.this.onPeerConnected(node);
                }
            });
        }

        @Override
        public void onPeerDisconnected(final NodeParcelable node) throws RemoteException {
            post(new Runnable() {
                @Override
                public void run() {
                    WearableListenerService.this.onPeerDisconnected(node);
                }
            });
        }

        @Override
        public void onConnectedNodes(final List<NodeParcelable> nodes) throws RemoteException {
            post(new Runnable() {
                @Override
                public void run() {
                    WearableListenerService.this.onConnectedNodes(new ArrayList<Node>(nodes));
                }
            });
        }

        @Override
        public void onConnectedCapabilityChanged(final CapabilityInfoParcelable capabilityInfo) throws RemoteException {
            post(new Runnable() {
                @Override
                public void run() {
                    WearableListenerService.this.onCapabilityChanged(capabilityInfo);
                }
            });
        }

        @Override
        public void onNotificationReceived(final AncsNotificationParcelable notification) throws RemoteException {
            post(new Runnable() {
                @Override
                public void run() {
                    WearableListenerService.this.onNotificationReceived(notification);
                }
            });
        }

        @Override
        public void onEntityUpdate(final AmsEntityUpdateParcelable update) throws RemoteException {
            post(new Runnable() {
                @Override
                public void run() {
                    WearableListenerService.this.onEntityUpdate(update);
                }
            });
        }

        @Override
        public void onChannelEvent(final ChannelEventParcelable channelEvent) throws RemoteException {
            post(new Runnable() {
                @Override
                public void run() {
                    switch (channelEvent.eventType) {
                        case 1:
                            WearableListenerService.this.onChannelOpened(new ChannelImpl(channelEvent.channel));
                            break;
                        case 2:
                            WearableListenerService.this.onChannelClosed(new ChannelImpl(channelEvent.channel), channelEvent.closeReason, channelEvent.appSpecificErrorCode);
                            break;
                        case 3:
                            WearableListenerService.this.onInputClosed(new ChannelImpl(channelEvent.channel), channelEvent.closeReason, channelEvent.appSpecificErrorCode);
                            break;
                        case 4:
                            WearableListenerService.this.onOutputClosed(new ChannelImpl(channelEvent.channel), channelEvent.closeReason, channelEvent.appSpecificErrorCode);
                            break;
                        default:
                            Log.w(TAG, "Unknown ChannelEvent.eventType");
                    }
                }
            });
        }
    }

    private class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
    }
}
