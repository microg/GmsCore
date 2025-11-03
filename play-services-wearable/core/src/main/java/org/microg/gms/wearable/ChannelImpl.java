/*
 * Copyright (C) 2023 microG Project Team
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
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.ChannelApi;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ChannelImpl implements Channel {

    private final String path;
    private final String nodeId;
    private final PipedInputStream inputStream;
    private final PipedOutputStream outputStream;
    private final List<ChannelApi.ChannelListener> listeners = new ArrayList<>();

    public ChannelImpl(String path, String nodeId) {
        this.path = path;
        this.nodeId = nodeId;
        this.inputStream = new PipedInputStream();
        try {
            this.outputStream = new PipedOutputStream(this.inputStream);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PendingResult<Status> addListener(GoogleApiClient client, ChannelApi.ChannelListener listener) {
        listeners.add(listener);
        return new ImmediatePendingResult<>(Status.SUCCESS);
    }

    @Override
    public PendingResult<Status> close(GoogleApiClient client) {
        return close(client, 0);
    }

    @Override
    public PendingResult<Status> close(GoogleApiClient client, int errorCode) {
        try {
            inputStream.close();
            outputStream.close();
            for (ChannelApi.ChannelListener listener : listeners) {
                listener.onChannelClosed(this, ChannelApi.ChannelListener.CLOSE_REASON_NORMAL, errorCode);
            }
        } catch (java.io.IOException e) {
            // Ignore
        }
        return new ImmediatePendingResult<>(Status.SUCCESS);
    }

    @Override
    public PendingResult<GetInputStreamResult> getInputStream(GoogleApiClient client) {
        return new ImmediatePendingResult<>(new GetInputStreamResult() {
            @Override
            public InputStream getInputStream() {
                return inputStream;
            }

            @Override
            public Status getStatus() {
                return Status.SUCCESS;
            }
        });
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public PendingResult<GetOutputStreamResult> getOutputStream(GoogleApiClient client) {
        return new ImmediatePendingResult<>(new GetOutputStreamResult() {
            @Override
            public OutputStream getOutputStream() {
                return outputStream;
            }

            @Override
            public Status getStatus() {
                return Status.SUCCESS;
            }
        });
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public PendingResult<Status> receiveFile(GoogleApiClient client, Uri uri, boolean append) {
        new Thread(() -> {
            try {
                java.io.FileOutputStream fos = new java.io.FileOutputStream(new java.io.File(uri.getPath()), append);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }).start();
        return new ImmediatePendingResult<>(Status.SUCCESS);
    }

    @Override
    public PendingResult<Status> removeListener(GoogleApiClient client, ChannelApi.ChannelListener listener) {
        listeners.remove(listener);
        return new ImmediatePendingResult<>(Status.SUCCESS);
    }

    @Override
    public PendingResult<Status> sendFile(GoogleApiClient client, Uri uri) {
        return sendFile(client, uri, 0, -1);
    }

    @Override
    public PendingResult<Status> sendFile(GoogleApiClient client, Uri uri, long startOffset, long length) {
        new Thread(() -> {
            try {
                java.io.FileInputStream fis = new java.io.FileInputStream(new java.io.File(uri.getPath()));
                if (startOffset > 0) {
                    fis.skip(startOffset);
                }
                byte[] buffer = new byte[1024];
                int len;
                long remaining = length;
                while ((len = fis.read(buffer)) != -1) {
                    if (length != -1) {
                        if (remaining <= 0) {
                            break;
                        }
                        if (len > remaining) {
                            len = (int) remaining;
                        }
                        remaining -= len;
                    }
                    outputStream.write(buffer, 0, len);
                }
                fis.close();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }).start();
        return new ImmediatePendingResult<>(Status.SUCCESS);
    }
}
