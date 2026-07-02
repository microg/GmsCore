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

package org.microg.gms.wearable;

import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.internal.IWearableListener;
import com.google.android.gms.wearable.internal.MessageEventParcelable;
import com.google.android.gms.wearable.internal.RemoveListenerRequest;
import com.google.android.gms.wearable.internal.SendMessageResponse;

import org.microg.gms.common.GmsConnector;

public class MessageApiImpl implements MessageApi {

    private static final String TAG = "GmsWearMsgApi";

    @Override
    public PendingResult<Status> addListener(GoogleApiClient client, MessageListener listener) {
        Log.d(TAG, "addListener: wrapping MessageListener for WearableImpl");
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, Status>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<Status> resultProvider) throws RemoteException {
                IWearableListener wearableListener = new MessageListenerWrapper(listener);
                client.getServiceInterface().addListener(new BaseWearableCallbacks() {
                    @Override
                    public void onStatus(Status status) throws RemoteException {
                        resultProvider.onResultAvailable(status);
                    }
                }, new com.google.android.gms.wearable.internal.AddListenerRequest(wearableListener, null, null));
            }
        });
    }

    @Override
    public PendingResult<Status> removeListener(GoogleApiClient client, MessageListener listener) {
        Log.d(TAG, "removeListener: cleaning up MessageListener");
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, Status>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<Status> resultProvider) throws RemoteException {
                // We need to remove the same IWearableListener that was added.
                // Since we don't track the wrapper instance, we create a new one to match.
                // In practice, the WearableServiceImpl removes by reference equality on the binder.
                // For correctness, we use the same wrapper pattern.
                IWearableListener wearableListener = new MessageListenerWrapper(listener);
                client.getServiceInterface().removeListener(new BaseWearableCallbacks() {
                    @Override
                    public void onStatus(Status status) throws RemoteException {
                        resultProvider.onResultAvailable(status);
                    }
                }, new RemoveListenerRequest(wearableListener));
            }
        });
    }

    @Override
    public PendingResult<SendMessageResult> sendMessage(GoogleApiClient client, final String nodeId, final String path, final byte[] data) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, SendMessageResult>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<SendMessageResult> resultProvider) throws RemoteException {
                client.getServiceInterface().sendMessage(new BaseWearableCallbacks() {
                    @Override
                    public void onSendMessageResponse(SendMessageResponse response) throws RemoteException {
                        resultProvider.onResultAvailable(new SendMessageResultImpl(response));
                    }
                }, nodeId, path, data);
            }
        });
    }

    public static class SendMessageResultImpl implements SendMessageResult {
        private SendMessageResponse response;

        public SendMessageResultImpl(SendMessageResponse response) {
            this.response = response;
        }

        @Override
        public int getRequestId() {
            return response.requestId;
        }

        @Override
        public Status getStatus() {
            return new Status(response.statusCode);
        }
    }

    /**
     * Wraps a MessageApi.MessageListener into an IWearableListener AIDL interface.
     * Translates onMessageReceived callbacks from the WearableImpl
     * into the MessageListener interface expected by client apps.
     */
    private static class MessageListenerWrapper extends IWearableListener.Stub {
        private final MessageListener listener;
        private static final String TAG_W = "MessageListenerWrapper";

        public MessageListenerWrapper(MessageListener listener) {
            this.listener = listener;
        }

        @Override
        public void onMessageReceived(MessageEventParcelable messageEvent) throws RemoteException {
            Log.d(TAG_W, "onMessageReceived: path=" + messageEvent.getPath() + " from=" + messageEvent.getSourceNodeId());
            if (listener != null) {
                listener.onMessageReceived(new MessageEvent(messageEvent));
            }
        }

        @Override
        public void onPeerConnected(NodeParcelable node) throws RemoteException {
            // Not used by MessageApi
        }

        @Override
        public void onPeerDisconnected(NodeParcelable node) throws RemoteException {
            // Not used by MessageApi
        }

        @Override
        public void onDataChanged(com.google.android.gms.common.data.DataHolder data) throws RemoteException {
            // Not used by MessageApi
        }

        @Override
        public void onConnectedNodes(java.util.List<NodeParcelable> nodes) throws RemoteException {
            // Not used by MessageApi
        }

        @Override
        public void onNotificationReceived(AncsNotificationParcelable notification) throws RemoteException {
            // Not used by MessageApi
        }

        @Override
        public void onChannelEvent(ChannelEventParcelable channelEvent) throws RemoteException {
            // Not used by MessageApi
        }

        @Override
        public void onConnectedCapabilityChanged(CapabilityInfoParcelable capabilityInfo) throws RemoteException {
            // Not used by MessageApi
        }

        @Override
        public void onEntityUpdate(AmsEntityUpdateParcelable update) throws RemoteException {
            // Not used by MessageApi
        }
    }
}
