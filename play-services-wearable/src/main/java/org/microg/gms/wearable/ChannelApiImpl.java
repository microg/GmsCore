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

import android.net.Uri;
import android.os.RemoteException;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.ChannelApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.internal.AddListenerRequest;
import com.google.android.gms.wearable.internal.ChannelParcelable;
import com.google.android.gms.wearable.internal.CloseChannelResponse;
import com.google.android.gms.wearable.internal.IChannelStreamCallbacks;
import com.google.android.gms.wearable.internal.OpenChannelResponse;
import com.google.android.gms.wearable.internal.RemoveListenerRequest;

import org.microg.gms.common.GmsConnector;

import java.io.FileDescriptor;

public class ChannelApiImpl implements ChannelApi {

    @Override
    public PendingResult<Status> addListener(GoogleApiClient client, ChannelListener listener) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, Status>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, GmsConnector.Callback.ResultProvider<Status> resultProvider) throws RemoteException {
                AddListenerRequest request = new AddListenerRequest(listener, null);
                client.getServiceInterface().addListener(new BaseWearableCallbacks() {
                    @Override
                    public void onStatus(Status status) throws RemoteException {
                        resultProvider.onResultAvailable(status);
                    }
                }, request);
            }
        });
    }

    @Override
    public PendingResult<OpenChannelResult> openChannel(GoogleApiClient client, final String nodeId, final String path) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, OpenChannelResult>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, GmsConnector.Callback.ResultProvider<OpenChannelResult> resultProvider) throws RemoteException {
                client.getServiceInterface().openChannel(new BaseWearableCallbacks() {
                    @Override
                    public void onOpenChannelResponse(OpenChannelResponse response) throws RemoteException {
                        resultProvider.onResultAvailable(new OpenChannelResultImpl(response));
                    }
                }, nodeId, path);
            }
        });
    }

    @Override
    public PendingResult<Status> removeListener(GoogleApiClient client, ChannelListener listener) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, Status>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, GmsConnector.Callback.ResultProvider<Status> resultProvider) throws RemoteException {
                RemoveListenerRequest request = new RemoveListenerRequest(listener);
                client.getServiceInterface().removeListener(new BaseWearableCallbacks() {
                    @Override
                    public void onStatus(Status status) throws RemoteException {
                        resultProvider.onResultAvailable(status);
                    }
                }, request);
            }
        });
    }

    public static class OpenChannelResultImpl implements OpenChannelResult {
        private final OpenChannelResponse response;

        public OpenChannelResultImpl(OpenChannelResponse response) {
            this.response = response;
        }

        @Override
        public Channel getChannel() {
            if (response.channel != null) {
                return new ChannelImpl(response.channel);
            }
            return null;
        }

        @Override
        public Status getStatus() {
            return new Status(response.statusCode);
        }
    }
}