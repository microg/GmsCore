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
import org.microg.gms.common.PublicApi

@PublicApi
interface MessageApi {
    /**
     * Registers a listener to be notified of received messages. Calls to this method should
     * balanced with [.removeListener] to avoid leaking
     * resources.
     *
     *
     * Callers wishing to be notified of events in the background should use [WearableListenerService].
     */
    fun addListener(client: GoogleApiClient?, listener: MessageListener?): PendingResult<Status?>?

    /**
     * Removes a message listener which was previously added through
     * [.addListener].
     */
    fun removeListener(client: GoogleApiClient?, listener: MessageListener?): PendingResult<Status?>?

    /**
     * Sends `byte[]` data to the specified node.
     *
     * @param nodeId identifier for a particular node on the Android Wear network. Valid targets
     * may be obtained through [NodeApi.getConnectedNodes]
     * or from the host in [DataItem.getUri].
     * @param path   identifier used to specify a particular endpoint at the receiving node
     * @param data   small array of information to pass to the target node. Generally not larger
     * than 100k
     */
    fun sendMessage(client: GoogleApiClient?, nodeId: String?,
                    path: String?, data: ByteArray?): PendingResult<SendMessageResult?>?

    /**
     * Used with [MessageApi.addListener] to receive
     * message events.
     *
     *
     * Callers wishing to be notified of events in the background should use
     * [WearableListenerService].
     */
    interface MessageListener {
        /**
         * Notification that a message has been received.
         */
        fun onMessageReceived(messageEvent: MessageEvent?)
    }

    /**
     * Contains the request id assigned to the message. On failure, the id will be
     * [MessageApi.UNKNOWN_REQUEST_ID] and the status will be unsuccessful.
     */
    interface SendMessageResult : Result {

        /**
         * @return an ID used to identify the sent message. If [.getStatus] is not
         * successful, this value will be [MessageApi.UNKNOWN_REQUEST_ID].
         */
        fun getRequestId(): Int
    }

    companion object {
        /**
         * A value returned by [SendMessageResult.getRequestId] when
         * [.sendMessage] fails.
         */
        const val UNKNOWN_REQUEST_ID = -1
    }
}