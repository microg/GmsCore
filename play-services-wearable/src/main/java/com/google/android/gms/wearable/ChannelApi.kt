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

import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.PendingResult
import com.google.android.gms.common.api.Result
import com.google.android.gms.common.api.Status
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Client interface for Wearable Channel API. Allows apps on a wearable device to send and receive
 * data from other wearable nodes.
 *
 *
 * Channels are bidirectional. Each side, both the initiator and the receiver may both read and
 * write to the channel by using [Channel.getOutputStream] and [Channel.getInputStream].
 * Once a channel is established, the API for the initiator and receiver are identical.
 *
 *
 * Channels are only available when the wearable nodes are connected. When the remote node
 * disconnects, all existing channels will be closed. Any listeners (added through [.addListener]
 * and any installed [WearableListenerService]) will be notified of the channel closing.
 */
interface ChannelApi {
    /**
     * Registers a listener to be notified of channel events. Calls to this method should be
     * balanced with calls to [.removeListener] to avoid
     * leaking resources.
     *
     *
     * Listener events will be called on the main thread, or the handler specified on `client`
     * when it was built (using [Builder.setHandler]).
     *
     *
     * Callers wishing to be notified of events in the background should use [WearableListenerService].
     *
     * @param client   a connected client
     * @param listener a listener which will be notified of changes to any channel
     */
    fun addListener(client: GoogleApiClient?, listener: ChannelListener?): PendingResult<Status?>?

    /**
     * Opens a channel to exchange data with a remote node.
     *
     *
     * Channel which are no longer needed should be closed using [Channel.close].
     *
     *
     * This call involves a network round trip, so may be long running. `client` must remain
     * connected during that time, or the request will be cancelled (like any other Play Services
     * API calls).
     *
     * @param client a connected client
     * @param nodeId the node ID of a wearable node, as returned from [NodeApi.getConnectedNodes]
     * @param path   an app-specific identifier for the channel
     */
    fun openChannel(client: GoogleApiClient?, nodeId: String?, path: String?): PendingResult<OpenChannelResult?>?

    /**
     * Removes a listener which was previously added through [.addListener].
     *
     * @param client   a connected client
     * @param listener a listener which was added using [.addListener]
     */
    fun removeListener(client: GoogleApiClient?, listener: ChannelListener?): PendingResult<Status?>?

    /**
     * A listener which will be notified on changes to channels.
     */
    interface ChannelListener {
        /**
         * Called when a channel is closed. This can happen through an explicit call to [Channel.close]
         * or [.close] on either side of the connection, or due to
         * disconnecting from the remote node.
         *
         * @param closeReason          the reason for the channel closing. One of [.CLOSE_REASON_DISCONNECTED],
         * [.CLOSE_REASON_REMOTE_CLOSE], or [.CLOSE_REASON_LOCAL_CLOSE].
         * @param appSpecificErrorCode the error code specified on [Channel.close],
         * or 0 if closeReason is [.CLOSE_REASON_DISCONNECTED].
         */
        fun onChannelClosed(channel: Channel?, closeReason: Int, appSpecificErrorCode: Int)

        /**
         * Called when a new channel is opened by a remote node.
         */
        fun onChannelOpened(channel: Channel?)

        /**
         * Called when the input side of a channel is closed.
         *
         * @param closeReason          the reason for the channel closing. One of [.CLOSE_REASON_DISCONNECTED],
         * [.CLOSE_REASON_REMOTE_CLOSE], [.CLOSE_REASON_LOCAL_CLOSE]
         * or [.CLOSE_REASON_NORMAL].
         * @param appSpecificErrorCode the error code specified on [Channel.close],
         * or 0 if closeReason is [.CLOSE_REASON_DISCONNECTED] or
         * [.CLOSE_REASON_NORMAL].
         */
        fun onInputClosed(channel: Channel?, @CloseReason closeReason: Int, appSpecificErrorCode: Int)

        /**
         * Called when the output side of a channel is closed.
         *
         * @param closeReason          the reason for the channel closing. One of [.CLOSE_REASON_DISCONNECTED],
         * [.CLOSE_REASON_REMOTE_CLOSE], [.CLOSE_REASON_LOCAL_CLOSE]
         * or [.CLOSE_REASON_NORMAL].
         * @param appSpecificErrorCode the error code specified on [Channel.close],
         * or 0 if closeReason is [.CLOSE_REASON_DISCONNECTED] or
         * [.CLOSE_REASON_NORMAL].
         */
        fun onOutputClosed(channel: Channel?, @CloseReason closeReason: Int, appSpecificErrorCode: Int)

        companion object {
            /**
             * Value passed to [.onChannelClosed], [.onInputClosed]
             * and [.onOutputClosed] when the closing is due to a remote node
             * being disconnected.
             */
            const val CLOSE_REASON_DISCONNECTED = 1

            /**
             * Value passed to [.onChannelClosed], [.onInputClosed]
             * and [.onOutputClosed] when the stream is closed due to the
             * local node calling [Channel.close] or [Channel.close].
             */
            const val CLOSE_REASON_LOCAL_CLOSE = 3

            /**
             * Value passed to [.onInputClosed] or [.onOutputClosed]
             * (but not [.onChannelClosed]), when the stream was closed under
             * normal conditions, e.g the whole file was read, or the OutputStream on the remote node
             * was closed normally.
             */
            const val CLOSE_REASON_NORMAL = 0

            /**
             * Value passed to [.onChannelClosed], [.onInputClosed]
             * and [.onOutputClosed] when the stream is closed due to the
             * remote node calling [Channel.close] or [Channel.close].
             */
            const val CLOSE_REASON_REMOTE_CLOSE = 2
        }
    }

    /**
     * An annotation for values passed to [ChannelListener.onChannelClosed],
     * and other methods on the [ChannelListener] interface. Annotated method parameters will
     * always take one of the following values:
     *
     *  * [ChannelListener.CLOSE_REASON_DISCONNECTED]
     *  * [ChannelListener.CLOSE_REASON_NORMAL]
     *  * [ChannelListener.CLOSE_REASON_LOCAL_CLOSE]
     *  * [ChannelListener.CLOSE_REASON_REMOTE_CLOSE]
     *
     */
    @Retention(RetentionPolicy.SOURCE)
    annotation class CloseReason

    /**
     * Result of [.openChannel].
     */
    interface OpenChannelResult : Result {
        /**
         * Returns the newly created channel, or `null`, if the connection couldn't be opened.
         */
        val channel: Channel?
    }

    companion object {
        /**
         * Channel action for use in listener filters.
         *
         * @see WearableListenerService
         */
        const val ACTION_CHANNEL_EVENT = "com.google.android.gms.wearable.CHANNEL_EVENT"
    }
}