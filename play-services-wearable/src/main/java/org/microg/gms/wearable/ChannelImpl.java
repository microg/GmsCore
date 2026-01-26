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
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.ChannelApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.internal.ChannelParcelable;

import org.microg.gms.common.GmsConnector;

import android.os.RemoteException;

public class ChannelImpl extends ChannelParcelable implements Channel {
    private static final String TAG = "GmsWearChannelImpl";

    public ChannelImpl(String token, String nodeId, String path) {
        super(token, nodeId, path);
    }

    public ChannelImpl(ChannelParcelable wrapped) {
        this(wrapped.token, wrapped.nodeId, wrapped.path);
    }


    @Override
    public PendingResult<Status> addListener(GoogleApiClient client, ChannelApi.ChannelListener listener) {
        return GmsConnector.call(client, Wearable.API, (wearable, resultProvider) -> {
            resultProvider.onResultAvailable(Status.SUCCESS);
        });
    }

    @Override
    public PendingResult<Status> close(GoogleApiClient client, int errorCode) {
        return GmsConnector.call(client, Wearable.API, (wearable, resultProvider) -> {
            wearable.getServiceInterface().closeChannelWithError(new BaseWearableCallbacks() {
                @Override
                public void onCloseChannelResponse(com.google.android.gms.wearable.internal.CloseChannelResponse response) throws RemoteException {
                    resultProvider.onResultAvailable(new Status(response.statusCode));
                }
            }, token, errorCode);
        });
    }

    @Override
    public PendingResult<Status> close(GoogleApiClient client) {
        return GmsConnector.call(client, Wearable.API, (wearable, resultProvider) -> {
            wearable.getServiceInterface().closeChannel(new BaseWearableCallbacks() {
                @Override
                public void onCloseChannelResponse(com.google.android.gms.wearable.internal.CloseChannelResponse response) throws RemoteException {
                    resultProvider.onResultAvailable(new Status(response.statusCode));
                }
            }, token);
        });
    }

    @Override
    public PendingResult<GetInputStreamResult> getInputStream(GoogleApiClient client) {
        return GmsConnector.call(client, Wearable.API, (wearable, resultProvider) -> {
            wearable.getServiceInterface().getChannelInputStream(new BaseWearableCallbacks() {
                @Override
                public void onGetChannelInputStreamResponse(com.google.android.gms.wearable.internal.GetChannelInputStreamResponse response) throws RemoteException {
                    resultProvider.onResultAvailable(new GetInputStreamResult() {
                        @Override
                        public java.io.InputStream getInputStream() {
                            return response.fd != null ? new android.os.ParcelFileDescriptor.AutoCloseInputStream(response.fd) : null;
                        }

                        @Override
                        public Status getStatus() {
                            return new Status(response.statusCode);
                        }

                        @Override
                        public void release() {
                            try {
                                if (response.fd != null) response.fd.close();
                            } catch (java.io.IOException ignored) {
                            }
                        }
                    });
                }
            }, null, token);
        });
    }

    @Override
    public PendingResult<GetOutputStreamResult> getOutputStream(GoogleApiClient client) {
        return GmsConnector.call(client, Wearable.API, (wearable, resultProvider) -> {
            wearable.getServiceInterface().getChannelOutputStream(new BaseWearableCallbacks() {
                @Override
                public void onGetChannelOutputStreamResponse(com.google.android.gms.wearable.internal.GetChannelOutputStreamResponse response) throws RemoteException {
                    resultProvider.onResultAvailable(new GetOutputStreamResult() {
                        @Override
                        public java.io.OutputStream getOutputStream() {
                            return response.fd != null ? new android.os.ParcelFileDescriptor.AutoCloseOutputStream(response.fd) : null;
                        }

                        @Override
                        public Status getStatus() {
                            return new Status(response.statusCode);
                        }

                        @Override
                        public void release() {
                            try {
                                if (response.fd != null) response.fd.close();
                            } catch (java.io.IOException ignored) {
                            }
                        }
                    });
                }
            }, null, token);
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
        return GmsConnector.call(client, Wearable.API, (wearable, resultProvider) -> {
            wearable.getServiceInterface().readChannelOutputFromFd(new BaseWearableCallbacks() {
                @Override
                public void onChannelReceiveFileResponse(com.google.android.gms.wearable.internal.ChannelReceiveFileResponse response) throws RemoteException {
                    resultProvider.onResultAvailable(new Status(response.statusCode));
                }
            }, token, null, 0, 0);
        });
    }

    @Override
    public PendingResult<Status> removeListener(GoogleApiClient client, ChannelApi.ChannelListener listener) {
        return GmsConnector.call(client, Wearable.API, (wearable, resultProvider) -> {
            resultProvider.onResultAvailable(Status.SUCCESS);
        });
    }

    @Override
    public PendingResult<Status> sendFile(GoogleApiClient client, Uri uri) {
        return sendFile(client, uri, 0, -1);
    }

    @Override
    public PendingResult<Status> sendFile(GoogleApiClient client, Uri uri, long startOffset, long length) {
        return GmsConnector.call(client, Wearable.API, (wearable, resultProvider) -> {
            wearable.getServiceInterface().writeChannelInputToFd(new BaseWearableCallbacks() {
                @Override
                public void onChannelSendFileResponse(com.google.android.gms.wearable.internal.ChannelSendFileResponse response) throws RemoteException {
                    resultProvider.onResultAvailable(new Status(response.statusCode));
                }
            }, token, null);
        });
    }
}
