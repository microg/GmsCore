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

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.internal.SendMessageResponse;

import org.microg.gms.common.GmsConnector;

public class MessageApiImpl implements MessageApi {
    @Override
    public PendingResult<Status> addListener(GoogleApiClient client, MessageListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PendingResult<Status> removeListener(GoogleApiClient client, MessageListener listener) {
        throw new UnsupportedOperationException();
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
}
