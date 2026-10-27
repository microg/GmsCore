/*
 * SPDX-FileCopyrightText: 2015, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.wearable;

import org.microg.wearable.proto.Connect;
import org.microg.wearable.proto.RootMessage;

import java.io.IOException;

public abstract class ServerMessageListener extends MessageListener {
    private Connect localConnect;
    private Connect remoteConnect;

    public ServerMessageListener(Connect localConnect) {
        this.localConnect = localConnect;
    }

    @Override
    public void onConnected(WearableConnection connection) {
        super.onConnected(connection);
        try {
            connection.writeMessage(new RootMessage.Builder().connect(localConnect).build());
        } catch (IOException ignored) {
            // Will disconnect soon
        }
    }

    @Override
    public void onDisconnected() {
        super.onDisconnected();
        remoteConnect = null;
    }

    @Override
    public void onConnect(Connect connect) {
        if (remoteConnect != null || connect.id == null || connect.id.isEmpty()
                || connect.id.equals(localConnect.id)) {
            throw new IllegalArgumentException("Invalid wearable peer identity");
        }
        int version = connect.peerVersion == null ? 0 : connect.peerVersion;
        int minimum = connect.peerMinimumVersion == null ? 0 : connect.peerMinimumVersion;
        int localVersion = localConnect.peerVersion == null ? 0 : localConnect.peerVersion;
        int localMinimum = localConnect.peerMinimumVersion == null ? 0 : localConnect.peerMinimumVersion;
        if (version < 0 || minimum < 0 || minimum > version
                || minimum > localVersion || version < localMinimum) {
            throw new IllegalArgumentException("Unsupported wearable protocol version");
        }
        this.remoteConnect = connect;
    }

    @Override
    public void onMessage(WearableConnection connection, RootMessage message) {
        // Connect must take precedence over any piggybacked application fields.
        if (message.connect != null) {
            onConnect(message.connect);
            return;
        }
        if (remoteConnect == null) {
            throw new IllegalArgumentException("Wearable peer has not negotiated a connection");
        }
        super.onMessage(connection, message);
    }

    public Connect getRemoteConnect() {
        return remoteConnect;
    }
}
