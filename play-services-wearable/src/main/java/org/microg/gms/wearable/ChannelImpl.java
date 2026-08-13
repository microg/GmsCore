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
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.ChannelApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.internal.ChannelParcelable;
import com.google.android.gms.wearable.internal.CloseChannelResponse;
import com.google.android.gms.wearable.internal.GetChannelInputStreamResponse;
import com.google.android.gms.wearable.internal.GetChannelOutputStreamResponse;

import org.microg.gms.common.GmsConnector;
import org.microg.gms.common.api.InstantPendingResult;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ChannelImpl extends ChannelParcelable implements Channel {
    public ChannelImpl(String token, String nodeId, String path) {
        super(token, nodeId, path);
    }

    public ChannelImpl(ChannelParcelable wrapped) {
        this(wrapped.token, wrapped.nodeId, wrapped.path);
    }

    @Override
    public PendingResult<Status> addListener(GoogleApiClient client, ChannelApi.ChannelListener listener) {
        return new InstantPendingResult<Status>(Status.CANCELED);
    }

    @Override
    public PendingResult<Status> close(GoogleApiClient client, final int errorCode) {
        if (client == null) {
            return new InstantPendingResult<Status>(Status.CANCELED);
        }
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, Status>() {
            @Override
            public void onClientAvailable(WearableClientImpl wearableClient, final ResultProvider<Status> resultProvider) throws RemoteException {
                wearableClient.getServiceInterface().closeChannelWithError(new BaseWearableCallbacks() {
                    @Override
                    public void onCloseChannelResponse(CloseChannelResponse response) throws RemoteException {
                        resultProvider.onResultAvailable(statusFromCode(response == null ? 8 : response.statusCode));
                    }
                }, token, errorCode);
            }
        });
    }

    @Override
    public PendingResult<Status> close(GoogleApiClient client) {
        if (client == null) {
            return new InstantPendingResult<Status>(Status.CANCELED);
        }
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, Status>() {
            @Override
            public void onClientAvailable(WearableClientImpl wearableClient, final ResultProvider<Status> resultProvider) throws RemoteException {
                wearableClient.getServiceInterface().closeChannel(new BaseWearableCallbacks() {
                    @Override
                    public void onCloseChannelResponse(CloseChannelResponse response) throws RemoteException {
                        resultProvider.onResultAvailable(statusFromCode(response == null ? 8 : response.statusCode));
                    }
                }, token);
            }
        });
    }

    @Override
    public PendingResult<GetInputStreamResult> getInputStream(GoogleApiClient client) {
        if (client == null) {
            return new InstantPendingResult<GetInputStreamResult>(
                    new GetInputStreamResultImpl(Status.CANCELED, null));
        }
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, GetInputStreamResult>() {
            @Override
            public void onClientAvailable(WearableClientImpl wearableClient, final ResultProvider<GetInputStreamResult> resultProvider) throws RemoteException {
                wearableClient.getServiceInterface().getChannelInputStream(new BaseWearableCallbacks() {
                    @Override
                    public void onGetChannelInputStreamResponse(GetChannelInputStreamResponse response) throws RemoteException {
                        if (response != null && response.statusCode == 0 && response.pfd != null) {
                            resultProvider.onResultAvailable(new GetInputStreamResultImpl(
                                    Status.SUCCESS,
                                    new ParcelFileDescriptor.AutoCloseInputStream(response.pfd)));
                        } else {
                            resultProvider.onResultAvailable(new GetInputStreamResultImpl(
                                    statusFromCode(response == null ? 8 : response.statusCode), null));
                        }
                    }
                }, null, token);
            }
        });
    }

    @Override
    public PendingResult<GetOutputStreamResult> getOutputStream(GoogleApiClient client) {
        if (client == null) {
            return new InstantPendingResult<GetOutputStreamResult>(
                    new GetOutputStreamResultImpl(Status.CANCELED, null));
        }
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, GetOutputStreamResult>() {
            @Override
            public void onClientAvailable(WearableClientImpl wearableClient, final ResultProvider<GetOutputStreamResult> resultProvider) throws RemoteException {
                wearableClient.getServiceInterface().getChannelOutputStream(new BaseWearableCallbacks() {
                    @Override
                    public void onGetChannelOutputStreamResponse(GetChannelOutputStreamResponse response) throws RemoteException {
                        if (response != null && response.statusCode == 0 && response.pfd != null) {
                            resultProvider.onResultAvailable(new GetOutputStreamResultImpl(
                                    Status.SUCCESS,
                                    new ParcelFileDescriptor.AutoCloseOutputStream(response.pfd)));
                        } else {
                            resultProvider.onResultAvailable(new GetOutputStreamResultImpl(
                                    statusFromCode(response == null ? 8 : response.statusCode), null));
                        }
                    }
                }, null, token);
            }
        });
    }

    public String getNodeId() {
        return nodeId;
    }

    @Override
    public String getPath() {
        return path;
    }

    public String getToken() {
        return token;
    }

    @Override
    public PendingResult<Status> receiveFile(GoogleApiClient client, Uri uri, boolean append) {
        return new InstantPendingResult<Status>(Status.CANCELED);
    }

    @Override
    public PendingResult<Status> removeListener(GoogleApiClient client, ChannelApi.ChannelListener listener) {
        return new InstantPendingResult<Status>(Status.CANCELED);
    }

    @Override
    public PendingResult<Status> sendFile(GoogleApiClient client, Uri uri) {
        return new InstantPendingResult<Status>(Status.CANCELED);
    }

    @Override
    public PendingResult<Status> sendFile(GoogleApiClient client, Uri uri, long startOffset, long length) {
        return new InstantPendingResult<Status>(Status.CANCELED);
    }

    private static Status statusFromCode(int statusCode) {
        if (statusCode == 0) {
            return Status.SUCCESS;
        }
        return new Status(statusCode);
    }

    static final class GetInputStreamResultImpl implements GetInputStreamResult {
        private final Status status;
        private final InputStream stream;

        GetInputStreamResultImpl(Status status, InputStream stream) {
            this.status = status;
            this.stream = stream;
        }

        @Override
        public Status getStatus() {
            return status;
        }

        @Override
        public InputStream getInputStream() {
            return stream;
        }

        @Override
        public void release() {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    static final class GetOutputStreamResultImpl implements GetOutputStreamResult {
        private final Status status;
        private final OutputStream stream;

        GetOutputStreamResultImpl(Status status, OutputStream stream) {
            this.status = status;
            this.stream = stream;
        }

        @Override
        public Status getStatus() {
            return status;
        }

        @Override
        public OutputStream getOutputStream() {
            return stream;
        }

        @Override
        public void release() {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
