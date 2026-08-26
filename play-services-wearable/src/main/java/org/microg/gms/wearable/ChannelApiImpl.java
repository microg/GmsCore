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
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.ChannelApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.internal.OpenChannelResponse;

import org.microg.gms.common.GmsConnector;
import org.microg.gms.common.api.InstantPendingResult;

public class ChannelApiImpl implements ChannelApi {
    @Override
    public PendingResult<Status> addListener(GoogleApiClient client, ChannelListener listener) {
        return new InstantPendingResult<Status>(Status.CANCELED);
    }

    @Override
    public PendingResult<OpenChannelResult> openChannel(GoogleApiClient client, final String nodeId, final String path) {
        if (client == null) {
            return new InstantPendingResult<OpenChannelResult>(
                    new OpenChannelResultImpl(Status.CANCELED, null));
        }
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, OpenChannelResult>() {
            @Override
            public void onClientAvailable(WearableClientImpl wearableClient, final ResultProvider<OpenChannelResult> resultProvider) throws RemoteException {
                wearableClient.getServiceInterface().openChannel(new BaseWearableCallbacks() {
                    @Override
                    public void onOpenChannelResponse(OpenChannelResponse response) throws RemoteException {
                        Status status = response == null
                                ? Status.INTERNAL_ERROR
                                : new Status(response.statusCode);
                        Channel channel = (response != null && response.statusCode == 0 && response.channel != null)
                                ? new ChannelImpl(response.channel)
                                : null;
                        resultProvider.onResultAvailable(new OpenChannelResultImpl(status, channel));
                    }
                }, nodeId, path);
            }
        });
    }

    @Override
    public PendingResult<Status> removeListener(GoogleApiClient client, ChannelListener listener) {
        return new InstantPendingResult<Status>(Status.CANCELED);
    }

    static final class OpenChannelResultImpl implements OpenChannelResult {
        private final Status status;
        private final Channel channel;

        OpenChannelResultImpl(Status status, Channel channel) {
            this.status = status;
            this.channel = channel;
        }

        @Override
        public Status getStatus() {
            return status;
        }

        @Override
        public Channel getChannel() {
            return channel;
        }
    }
}
