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
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;

import org.microg.gms.common.PublicApi;

@PublicApi
public interface MessageApi {

    /**
     * A value returned by {@link SendMessageResult#getRequestId()} when
     * {@link #sendMessage(GoogleApiClient, String, String, byte[])} fails.
     */
    int UNKNOWN_REQUEST_ID = -1;

    /**
     * Registers a listener to be notified of received messages. Calls to this method should
     * balanced with {@link #removeListener(GoogleApiClient, MessageListener)} to avoid leaking
     * resources.
     * <p/>
     * Callers wishing to be notified of events in the background should use {@link WearableListenerService}.
     */
    PendingResult<Status> addListener(GoogleApiClient client, MessageListener listener);

    /**
     * Removes a message listener which was previously added through
     * {@link #addListener(GoogleApiClient, MessageListener)}.
     */
    PendingResult<Status> removeListener(GoogleApiClient client, MessageListener listener);

    /**
     * Sends {@code byte[]} data to the specified node.
     *
     * @param nodeId identifier for a particular node on the Android Wear network. Valid targets
     *               may be obtained through {@link NodeApi#getConnectedNodes(GoogleApiClient)}
     *               or from the host in {@link DataItem#getUri()}.
     * @param path   identifier used to specify a particular endpoint at the receiving node
     * @param data   small array of information to pass to the target node. Generally not larger
     *               than 100k
     */
    PendingResult<SendMessageResult> sendMessage(GoogleApiClient client, String nodeId,
                                                 String path, byte[] data);

    /**
     * Used with {@link MessageApi#addListener(GoogleApiClient, MessageListener)} to receive
     * message events.
     * <p/>
     * Callers wishing to be notified of events in the background should use
     * {@link WearableListenerService}.
     */
    interface MessageListener {
        /**
         * Notification that a message has been received.
         */
        void onMessageReceived(MessageEvent messageEvent);
    }

    /**
     * Contains the request id assigned to the message. On failure, the id will be
     * {@link MessageApi#UNKNOWN_REQUEST_ID} and the status will be unsuccessful.
     */
    interface SendMessageResult extends Result {
        /**
         * @return an ID used to identify the sent message. If {@link #getStatus()} is not
         * successful, this value will be {@link MessageApi#UNKNOWN_REQUEST_ID}.
         */
        int getRequestId();
    }
}
