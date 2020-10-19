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
package com.google.android.gms.wearable

import android.net.Uri
import android.os.Parcelable
import com.google.android.gms.common.api.*
import java.io.InputStream
import java.io.OutputStream

/**
 * A channel created through [ChannelApi.openChannel].
 *
 *
 * The implementation of this interface is parcelable and immutable, and implements reasonable [.equals]
 * and [.hashCode] methods, so can be used in collections.
 */
interface Channel : Parcelable {
    fun addListener(client: GoogleApiClient?, listener: ChannelApi.ChannelListener?): PendingResult<Status?>?
    fun close(client: GoogleApiClient?, errorCode: Int): PendingResult<Status?>?
    fun close(client: GoogleApiClient?): PendingResult<Status?>?
    fun getInputStream(client: GoogleApiClient?): PendingResult<GetInputStreamResult?>?
    fun getOutputStream(client: GoogleApiClient?): PendingResult<GetOutputStreamResult?>?
    fun getPath(): String?
    fun receiveFile(client: GoogleApiClient?, uri: Uri?, append: Boolean): PendingResult<Status?>?
    fun removeListener(client: GoogleApiClient?, listener: ChannelApi.ChannelListener?): PendingResult<Status?>?
    fun sendFile(client: GoogleApiClient?, uri: Uri?): PendingResult<Status?>?
    fun sendFile(client: GoogleApiClient?, uri: Uri?, startOffset: Long, length: Long): PendingResult<Status?>?
    interface GetInputStreamResult : Releasable, Result {
        /**
         * Returns an input stream which can read data from the remote node. The stream should be
         * closed when no longer needed. This method will only return `null` if this result's
         * [status][.getStatus] was not [success][Status.isSuccess].
         *
         *
         * The returned stream will throw [IOException] on read if any connection errors
         * occur. This exception might be a [ChannelIOException].
         *
         *
         * Since data for this stream comes over the network, reads may block for a long time.
         *
         *
         * Multiple calls to this method will return the same instance.
         */
        fun getInputStream(): InputStream?
    }

    interface GetOutputStreamResult : Releasable, Result {
        /**
         * Returns an output stream which can send data to a remote node. The stream should be
         * closed when no longer needed. This method will only return `null` if this result's
         * [status][.getStatus] was not [success][Status.isSuccess].
         *
         *
         * The returned stream will throw [IOException] on read if any connection errors
         * occur. This exception might be a [ChannelIOException].
         *
         *
         * Since data for this stream comes over the network, reads may block for a long time.
         *
         *
         * Data written to this stream is buffered. If you wish to send the current data without
         * waiting for the buffer to fill up, [flush][OutputStream.flush] the stream.
         *
         *
         * Multiple calls to this method will return the same instance.
         */
        fun getOutputStream(): OutputStream?
    }
}