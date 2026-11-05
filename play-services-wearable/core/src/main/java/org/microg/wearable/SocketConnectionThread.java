/*
 * SPDX-FileCopyrightText: 2015, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.wearable;

import android.content.Context;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public abstract class SocketConnectionThread extends Thread {

    private volatile SocketWearableConnection wearableConnection;
    protected volatile boolean stopped;

    private SocketConnectionThread() {
        super();
    }

    protected void setWearableConnection(SocketWearableConnection wearableConnection) {
        this.wearableConnection = wearableConnection;
    }

    public SocketWearableConnection getWearableConnection() {
        return wearableConnection;
    }

    public abstract void close();

    static ServerSocket createEmulatorListener(int port, Runnable authorize) throws IOException {
        authorize.run();
        // This unauthenticated transport is reached through the local ADB bridge.
        return new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"));
    }

    public static SocketConnectionThread serverListen(Context context, final int port, final WearableConnection.Listener listener) {
        return serverListen(port, listener, () -> EmulatorTransportPolicy.requireAllowed(context));
    }

    // Package-private injection lets socket tests exercise denial without relying on host properties.
    static SocketConnectionThread serverListen(final int port, final WearableConnection.Listener listener, Runnable authorize) {
        authorize.run();
        return new SocketConnectionThread() {
            private volatile ServerSocket serverSocket = null;
            private final Object listenerLock = new Object();

            @Override
            public void close() {
                synchronized (listenerLock) {
                    stopped = true;
                    if (serverSocket != null) {
                        try {
                            serverSocket.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
                interrupt();
                SocketWearableConnection connection = getWearableConnection();
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (IOException ignored) {
                    }
                }
            }

            @Override
            public void run() {
                try {
                    synchronized (listenerLock) {
                        if (stopped) return;
                        serverSocket = createEmulatorListener(port, authorize);
                    }
                    Socket socket;
                    while (!stopped && (socket = serverSocket.accept()) != null) {
                        SocketWearableConnection connection = new SocketWearableConnection(socket, listener);
                        setWearableConnection(connection);
                        if (stopped) {
                            connection.close();
                            break;
                        }
                        connection.run();
                    }
                } catch (IOException | SecurityException e) {
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

    public static SocketConnectionThread clientConnect(Context context, final int port, final WearableConnection.Listener listener) {
        EmulatorTransportPolicy.requireAllowed(context);
        return new SocketConnectionThread() {
            private volatile Socket socket;

            @Override
            public void close() {
                stopped = true;
                interrupt();
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException ignored) {
                    }
                }
            }

            @Override
            public void run() {
                try {
                    EmulatorTransportPolicy.requireAllowed(context);
                    socket = new Socket("127.0.0.1", port);
                    if (stopped) return;
                    SocketWearableConnection connection = new SocketWearableConnection(socket, listener);
                    setWearableConnection(connection);
                    connection.run();
                } catch (IOException | SecurityException e) {
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
