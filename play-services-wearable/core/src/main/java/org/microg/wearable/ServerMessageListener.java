/*
 * Copyright (C) 2024 microG Project Team
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

package org.microg.wearable;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.wearable.ConnectionConfiguration;

import org.microg.wearable.proto.Connect;
import org.microg.wearable.proto.RootMessage;

/**
 * Base listener for wearable protocol messages.
 * Handles the initial Connect handshake and delegates to WearableImpl.
 */
public class ServerMessageListener {
    private static final String TAG = "WearMsgListener";

    protected final Context context;
    protected final String localNodeId;
    protected final String localNodeName;
    protected final ConnectionConfiguration config;

    public ServerMessageListener(Context context, ConnectionConfiguration config,
                                  String localNodeName, String localNodeId) {
        this.context = context;
        this.config = config;
        this.localNodeName = localNodeName;
        this.localNodeId = localNodeId;
    }

    /**
     * Called when a message is received from the wearable device.
     */
    public void onMessageReceived(WearableConnection connection, RootMessage message) {
        if (message.connect != null) {
            onConnectReceived(connection, message.connect);
        } else if (message.disconnect != null) {
            onDisconnectReceived(connection, message.connect);
        } else if (message.heartbeat != null) {
            // Heartbeat - connection is alive
            connection.writeMessage(new RootMessage.Builder().heartbeat(message.heartbeat).build());
        } else {
            Log.w(TAG, "Unknown message type: " + message);
        }
    }

    /**
     * Called when a Connect message is received (handshake).
     */
    protected void onConnectReceived(WearableConnection connection, Connect connect) {
        Log.i(TAG, "Connect received from " + connect.id + " (" + connect.name + ")");

        // Send our Connect response
        Connect response = new Connect.Builder()
                .id(localNodeId)
                .name(localNodeName)
                .build();
        connection.writeMessage(new RootMessage.Builder().connect(response).build());
    }

    /**
     * Called when a Disconnect message is received.
     */
    protected void onDisconnectReceived(WearableConnection connection, Connect connect) {
        Log.i(TAG, "Disconnect received");
    }

    /**
     * Called when the connection is closed.
     */
    public void onConnectionClosed(WearableConnection connection) {
        Log.i(TAG, "Connection closed");
    }
}
