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

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.Builder;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Client interface for Wearable Channel API. Allows apps on a wearable device to send and receive
 * data from other wearable nodes.
 * <p/>
 * Channels are bidirectional. Each side, both the initiator and the receiver may both read and
 * write to the channel by using {@link Channel#getOutputStream(GoogleApiClient)} and {@link Channel#getInputStream(GoogleApiClient)}.
 * Once a channel is established, the API for the initiator and receiver are identical.
 * <p/>
 * Channels are only available when the wearable nodes are connected. When the remote node
 * disconnects, all existing channels will be closed. Any listeners (added through {@link #addListener(GoogleApiClient, ChannelListener)}
 * and any installed {@link WearableListenerService}) will be notified of the channel closing.
 */
public interface ChannelApi {
    /**
     * Channel action for use in listener filters.
     *
     * @see WearableListenerService
     */
    String ACTION_CHANNEL_EVENT = "com.google.android.gms.wearable.CHANNEL_EVENT";

    /**
     * Registers a listener to be notified of channel events. Calls to this method should be
     * balanced with calls to {@link #removeListener(GoogleApiClient, ChannelListener)} to avoid
     * leaking resources.
     * <p/>
     * Listener events will be called on the main thread, or the handler specified on {@code client}
     * when it was built (using {@link Builder#setHandler(Handler)}).
     * <p/>
     * Callers wishing to be notified of events in the background should use {@link WearableListenerService}.
     *
     * @param client   a connected client
     * @param listener a listener which will be notified of changes to any channel
     */
    PendingResult<Status> addListener(GoogleApiClient client, ChannelListener listener);

    /**
     * Opens a channel to exchange data with a remote node.
     * <p/>
     * Channel which are no longer needed should be closed using {@link Channel#close(GoogleApiClient)}.
     * <p/>
     * This call involves a network round trip, so may be long running. {@code client} must remain
     * connected during that time, or the request will be cancelled (like any other Play Services
     * API calls).
     *
     * @param client a connected client
     * @param nodeId the node ID of a wearable node, as returned from {@link NodeApi#getConnectedNodes(GoogleApiClient)}
     * @param path   an app-specific identifier for the channel
     */
    PendingResult<OpenChannelResult> openChannel(GoogleApiClient client, String nodeId, String path);

    /**
     * Removes a listener which was previously added through {@link #addListener(GoogleApiClient, ChannelListener)}.
     *
     * @param client   a connected client
     * @param listener a listener which was added using {@link #addListener(GoogleApiClient, ChannelListener)}
     */
    PendingResult<Status> removeListener(GoogleApiClient client, ChannelListener listener);

    /**
     * A listener which will be notified on changes to channels.
     */
    interface ChannelListener {
        /**
         * Value passed to {@link #onChannelClosed(Channel, int, int)}, {@link #onInputClosed(Channel, int, int)}
         * and {@link #onOutputClosed(Channel, int, int)} when the closing is due to a remote node
         * being disconnected.
         */
        int CLOSE_REASON_DISCONNECTED = 1;

        /**
         * Value passed to {@link #onChannelClosed(Channel, int, int)}, {@link #onInputClosed(Channel, int, int)}
         * and {@link #onOutputClosed(Channel, int, int)} when the stream is closed due to the
         * local node calling {@link Channel#close(GoogleApiClient)} or {@link Channel#close(GoogleApiClient, int)}.
         */
        int CLOSE_REASON_LOCAL_CLOSE = 3;

        /**
         * Value passed to {@link #onInputClosed(Channel, int, int)} or {@link #onOutputClosed(Channel, int, int)}
         * (but not {@link #onChannelClosed(Channel, int, int)}), when the stream was closed under
         * normal conditions, e.g the whole file was read, or the OutputStream on the remote node
         * was closed normally.
         */
        int CLOSE_REASON_NORMAL = 0;

        /**
         * Value passed to {@link #onChannelClosed(Channel, int, int)}, {@link #onInputClosed(Channel, int, int)}
         * and {@link #onOutputClosed(Channel, int, int)} when the stream is closed due to the
         * remote node calling {@link Channel#close(GoogleApiClient)} or {@link Channel#close(GoogleApiClient, int)}.
         */
        int CLOSE_REASON_REMOTE_CLOSE = 2;

        /**
         * Called when a channel is closed. This can happen through an explicit call to {@link Channel#close(GoogleApiClient)}
         * or {@link #close(GoogleApiClient, int)} on either side of the connection, or due to
         * disconnecting from the remote node.
         *
         * @param closeReason          the reason for the channel closing. One of {@link #CLOSE_REASON_DISCONNECTED},
         *                             {@link #CLOSE_REASON_REMOTE_CLOSE}, or {@link #CLOSE_REASON_LOCAL_CLOSE}.
         * @param appSpecificErrorCode the error code specified on {@link Channel#close(GoogleApiClient, int)},
         *                             or 0 if closeReason is {@link #CLOSE_REASON_DISCONNECTED}.
         */
        void onChannelClosed(Channel channel, int closeReason, int appSpecificErrorCode);

        /**
         * Called when a new channel is opened by a remote node.
         */
        void onChannelOpened(Channel channel);

        /**
         * Called when the input side of a channel is closed.
         *
         * @param closeReason          the reason for the channel closing. One of {@link #CLOSE_REASON_DISCONNECTED},
         *                             {@link #CLOSE_REASON_REMOTE_CLOSE}, {@link #CLOSE_REASON_LOCAL_CLOSE}
         *                             or {@link #CLOSE_REASON_NORMAL}.
         * @param appSpecificErrorCode the error code specified on {@link Channel#close(GoogleApiClient, int)},
         *                             or 0 if closeReason is {@link #CLOSE_REASON_DISCONNECTED} or
         *                             {@link #CLOSE_REASON_NORMAL}.
         */
        void onInputClosed(Channel channel, @CloseReason int closeReason, int appSpecificErrorCode);

        /**
         * Called when the output side of a channel is closed.
         *
         * @param closeReason          the reason for the channel closing. One of {@link #CLOSE_REASON_DISCONNECTED},
         *                             {@link #CLOSE_REASON_REMOTE_CLOSE}, {@link #CLOSE_REASON_LOCAL_CLOSE}
         *                             or {@link #CLOSE_REASON_NORMAL}.
         * @param appSpecificErrorCode the error code specified on {@link Channel#close(GoogleApiClient, int)},
         *                             or 0 if closeReason is {@link #CLOSE_REASON_DISCONNECTED} or
         *                             {@link #CLOSE_REASON_NORMAL}.
         */
        void onOutputClosed(Channel channel, @CloseReason int closeReason, int appSpecificErrorCode);
    }

    /**
     * An annotation for values passed to {@link ChannelListener#onChannelClosed(Channel, int, int)},
     * and other methods on the {@link ChannelListener} interface. Annotated method parameters will
     * always take one of the following values:
     * <ul>
     * <li>{@link ChannelListener#CLOSE_REASON_DISCONNECTED}</li>
     * <li>{@link ChannelListener#CLOSE_REASON_NORMAL}</li>
     * <li>{@link ChannelListener#CLOSE_REASON_LOCAL_CLOSE}</li>
     * <li>{@link ChannelListener#CLOSE_REASON_REMOTE_CLOSE}</li>
     * </ul>
     */
    @Retention(RetentionPolicy.SOURCE)
    @interface CloseReason {
    }

    /**
     * Result of {@link #openChannel(GoogleApiClient, String, String)}.
     */
    interface OpenChannelResult extends Result {
        /**
         * Returns the newly created channel, or {@code null}, if the connection couldn't be opened.
         */
        Channel getChannel();
    }
}
