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

import android.net.Uri;
import android.os.Parcelable;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Releasable;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A channel created through {@link ChannelApi#openChannel(GoogleApiClient, String, String)}.
 * <p/>
 * The implementation of this interface is parcelable and immutable, and implements reasonable {@link #equals(Object)}
 * and {@link #hashCode()} methods, so can be used in collections.
 */
public interface Channel extends Parcelable {

    PendingResult<Status> addListener(GoogleApiClient client, ChannelApi.ChannelListener listener);

    PendingResult<Status> close(GoogleApiClient client, int errorCode);

    PendingResult<Status> close(GoogleApiClient client);

    PendingResult<GetInputStreamResult> getInputStream(GoogleApiClient client);

    PendingResult<GetOutputStreamResult> getOutputStream(GoogleApiClient client);

    String getPath();

    PendingResult<Status> receiveFile(GoogleApiClient client, Uri uri, boolean append);

    PendingResult<Status> removeListener(GoogleApiClient client, ChannelApi.ChannelListener listener);

    PendingResult<Status> sendFile(GoogleApiClient client, Uri uri);

    PendingResult<Status> sendFile(GoogleApiClient client, Uri uri, long startOffset, long length);

    interface GetInputStreamResult extends Releasable, Result {
        /**
         * Returns an input stream which can read data from the remote node. The stream should be
         * closed when no longer needed. This method will only return {@code null} if this result's
         * {@linkplain #getStatus() status} was not {@linkplain Status#isSuccess() success}.
         * <p/>
         * The returned stream will throw {@link IOException} on read if any connection errors
         * occur. This exception might be a {@link ChannelIOException}.
         * <p/>
         * Since data for this stream comes over the network, reads may block for a long time.
         * <p/>
         * Multiple calls to this method will return the same instance.
         */
        InputStream getInputStream();
    }

    interface GetOutputStreamResult extends Releasable, Result {
        /**
         * Returns an output stream which can send data to a remote node. The stream should be
         * closed when no longer needed. This method will only return {@code null} if this result's
         * {@linkplain #getStatus() status} was not {@linkplain Status#isSuccess() success}.
         * <p/>
         * The returned stream will throw {@link IOException} on read if any connection errors
         * occur. This exception might be a {@link ChannelIOException}.
         * <p/>
         * Since data for this stream comes over the network, reads may block for a long time.
         * <p/>
         * Data written to this stream is buffered. If you wish to send the current data without
         * waiting for the buffer to fill up, {@linkplain OutputStream#flush() flush} the stream.
         * <p/>
         * Multiple calls to this method will return the same instance.
         */
        OutputStream getOutputStream();
    }
}
