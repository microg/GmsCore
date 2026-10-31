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

import android.content.IntentFilter;
import android.os.RemoteException;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.internal.AddListenerRequest;
import com.google.android.gms.wearable.internal.IWearableListener;
import com.google.android.gms.wearable.internal.MessageEventParcelable;
import com.google.android.gms.wearable.internal.RemoveListenerRequest;
import com.google.android.gms.wearable.internal.SendMessageResponse;

import org.microg.gms.common.GmsConnector;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageApiImpl implements MessageApi {
    private final Map<MessageListener, IWearableListener> listenerWrappers = new ConcurrentHashMap<MessageListener, IWearableListener>();

    @Override
    public PendingResult<Status> addListener(GoogleApiClient client, final MessageListener listener) {
        final IWearableListener wrapper = new MessageListenerWrapper(listener);
        listenerWrappers.put(listener, wrapper);
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, Status>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<Status> resultProvider) throws RemoteException {
                client.getServiceInterface().addListener(new BaseWearableCallbacks() {
                    @Override
                    public void onStatus(Status status) throws RemoteException {
                        resultProvider.onResultAvailable(status);
                    }
                }, new AddListenerRequest(wrapper, new IntentFilter[0], null));
            }
        });
    }

    @Override
    public PendingResult<Status> removeListener(GoogleApiClient client, final MessageListener listener) {
        final IWearableListener wrapper = listenerWrappers.remove(listener);
        if (wrapper == null) {
            return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, Status>() {
                @Override
                public void onClientAvailable(WearableClientImpl client, final ResultProvider<Status> resultProvider) throws RemoteException {
                    resultProvider.onResultAvailable(Status.SUCCESS);
                }
            });
        }
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, Status>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<Status> resultProvider) throws RemoteException {
                client.getServiceInterface().removeListener(new BaseWearableCallbacks() {
                    @Override
                    public void onStatus(Status status) throws RemoteException {
                        resultProvider.onResultAvailable(status);
                    }
                }, new RemoveListenerRequest(wrapper));
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

    private static class MessageListenerWrapper extends BaseWearableListener {
        private final MessageListener listener;

        MessageListenerWrapper(MessageListener listener) {
            this.listener = listener;
        }

        @Override
        public void onMessageReceived(MessageEventParcelable messageEvent) throws RemoteException {
            listener.onMessageReceived(messageEvent);
        }
    }
}
