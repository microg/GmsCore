/*
 * Copyright 2013-2015 µg Project Team
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

package com.google.android.gms.gcm;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.io.IOException;

public class GoogleCloudMessaging {

    /**
     * The GCM {@link #register(String...)} and {@link #unregister()} methods are blocking. You
     * should not run them in the main thread or in broadcast receivers.
     */
    public static final String ERROR_MAIN_THREAD = "MAIN_THREAD";

    /**
     * The device can't read the response, or there was a 500/503 from the server that can be
     * retried later. The application should use exponential back off and retry.
     */
    public static final String ERROR_SERVICE_NOT_AVAILABLE = "SERVICE_NOT_AVAILABLE";

    /**
     * Returned by {@link #getMessageType(Intent)} to indicate that the server deleted some
     * pending messages because they were collapsible.
     */
    public static final String MESSAGE_TYPE_DELETED = "deleted_messages";

    /**
     * Returned by {@link #getMessageType(Intent)} to indicate a regular message.
     */
    public static final String MESSAGE_TYPE_MESSAGE = "gcm";

    /**
     * Returned by {@link #getMessageType(Intent)} to indicate a send error. The intent includes
     * the message ID of the message and an error code.
     */
    public static final String MESSAGE_TYPE_SEND_ERROR = "send_error";

    /**
     * Return the singleton instance of GCM.
     */
    public static synchronized GoogleCloudMessaging getInstance(Context context) {
        return null;
    }

    /**
     * Must be called when your application is done using GCM, to release internal resources.
     */
    public void close() {

    }

    /**
     * Return the message type from an intent passed into a client app's broadcast receiver.
     * There are two general categories of messages passed from the server: regular GCM messages,
     * and special GCM status messages. The possible types are:
     * <ul>
     * <li>{@link #MESSAGE_TYPE_MESSAGE}—regular message from your server.</li>
     * <li>{@link #MESSAGE_TYPE_DELETED}—special status message indicating that some messages have been collapsed by GCM.</li>
     * <li>{@link #MESSAGE_TYPE_SEND_ERROR}—special status message indicating that there were errors sending one of the messages.</li>
     * </ul>
     * You can use this method to filter based on message type. Since it is likely that GCM will
     * be extended in the future with new message types, just ignore any message types you're not
     * interested in, or that you don't recognize.
     *
     * @param intent
     * @return
     */
    public String getMessageType(Intent intent) {
        return null;
    }

    public String register(String... senderIds) {
        return null;
    }

    public void send(String to, String msgId, long timeToLive, Bundle data) {

    }

    public void send(String to, String msgId, Bundle data) {

    }

    /**
     * Unregister the application. Calling unregister() stops any messages from the server.
     * This is a blocking call—you shouldn't call it from the UI thread.
     * You should rarely (if ever) need to call this method. Not only is it expensive in terms of
     * resources, but it invalidates your registration ID, which you should never change
     * unnecessarily. A better approach is to simply have your server stop sending messages.
     * Only use unregister if you want to change your sender ID.
     *
     * @throws IOException if we can't connect to server to unregister.
     */
    public void unregister() throws IOException {

    }
}
