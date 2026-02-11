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
        this.remoteConnect = connect;
    }

    public Connect getRemoteConnect() {
        return remoteConnect;
    }
}
