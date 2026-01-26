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

package org.microg.gms.wearable;

import android.os.RemoteException;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.ChannelApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.internal.OpenChannelResponse;

import org.microg.gms.common.GmsConnector;

public class ChannelApiImpl implements ChannelApi {
    @Override
    public PendingResult<Status> addListener(GoogleApiClient client, ChannelListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PendingResult<OpenChannelResult> openChannel(GoogleApiClient client, final String nodeId, final String path) {
        return GmsConnector.call(client, Wearable.API, (wearable, resultProvider) -> {
            wearable.getServiceInterface().openChannel(new BaseWearableCallbacks() {
                @Override
                public void onOpenChannelResponse(OpenChannelResponse response) throws RemoteException {
                    resultProvider.onResultAvailable(new OpenChannelResultImpl(new Status(response.statusCode), response.channel));
                }
            }, nodeId, path);
        });
    }

    @Override
    public PendingResult<Status> removeListener(GoogleApiClient client, ChannelListener listener) {
        throw new UnsupportedOperationException();
    }

    private static class OpenChannelResultImpl implements OpenChannelResult {
        private final Status status;
        private final com.google.android.gms.wearable.Channel channel;

        private OpenChannelResultImpl(Status status, com.google.android.gms.wearable.internal.ChannelParcelable channel) {
            this.status = status;
            this.channel = channel != null ? new ChannelImpl(channel) : null;
        }

        @Override
        public com.google.android.gms.wearable.Channel getChannel() {
            return channel;
        }

        @Override
        public Status getStatus() {
            return status;
        }
    }
}
