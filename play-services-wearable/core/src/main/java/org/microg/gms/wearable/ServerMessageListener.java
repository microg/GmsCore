/*
 * SPDX-FileCopyrightText: 2015, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import org.microg.gms.wearable.proto.Connect;
import org.microg.gms.wearable.proto.RootMessage;

import java.io.IOException;

public abstract class ServerMessageListener extends MessageListener {
    private Connect localConnect;
    private Connect remoteConnect;

    private boolean handshakeAlreadyDone = false;

    public ServerMessageListener(Connect localConnect) {
        this.localConnect = localConnect;
    }

    public void markHandshakeAlreadyDone() {
        this.handshakeAlreadyDone = true;
    }

    @Override
    public void onConnected(WearableConnection connection) {
        super.onConnected(connection);
        if (!handshakeAlreadyDone) {
            try {
                connection.writeMessage(new RootMessage.Builder().connect(localConnect).build());
            } catch (IOException ignored) {}
        }
    }

    @Override
    public void onDisconnected() {
        super.onDisconnected();
        remoteConnect = null;
    }

    @Override
    public void onConnect(Connect connect) {
        this.remoteConnect = connect;
    }

    public Connect getRemoteConnect() {
        return remoteConnect;
    }
}
