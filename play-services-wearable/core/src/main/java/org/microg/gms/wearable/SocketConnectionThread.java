/*
 * SPDX-FileCopyrightText: 2015, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public abstract class SocketConnectionThread extends Thread {

    private SocketWearableConnection wearableConnection;

    private SocketConnectionThread() {
        super();
    }

    protected void setWearableConnection(org.microg.gms.wearable.SocketWearableConnection wearableConnection) {
        this.wearableConnection = wearableConnection;
    }

    public SocketWearableConnection getWearableConnection() {
        return wearableConnection;
    }

    public abstract void close();

    public static SocketConnectionThread serverListen(final int port, final WearableConnection.Listener listener) {
        return new SocketConnectionThread() {
            private ServerSocket serverSocket = null;

            @Override
            public void close() {
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException ignored) {
                    }
                    serverSocket = null;
                }
            }

            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(port);
                    Socket socket;
                    while ((socket = serverSocket.accept()) != null && !Thread.interrupted()) {
                        SocketWearableConnection connection = new SocketWearableConnection(socket, listener);
                        setWearableConnection(connection);
                        connection.run();
                    }
                } catch (IOException e) {
                    // quit
                } finally {
                    try {
                        if (serverSocket != null) serverSocket.close();
                    } catch (IOException e) {
                    }
                }
            }
        };
    }

    public static SocketConnectionThread clientConnect(final int port, final WearableConnection.Listener listener) {
        return new SocketConnectionThread() {
            private Socket socket;

            @Override
            public void close() {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException ignored) {
                    }
                    socket = null;
                }
            }

            @Override
            public void run() {
                try {
                    socket = new Socket("127.0.0.1", port);
                    SocketWearableConnection connection = new SocketWearableConnection(socket, listener);
                    setWearableConnection(connection);
                    connection.run();
                } catch (IOException e) {
                    // quit
                } finally {
                    try {
                        if (socket != null) socket.close();
                    } catch (IOException e) {
                    }
                }
            }
        };
    }
}
