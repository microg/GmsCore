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

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TCP server thread for WearOS device communication.
 * Listens on a specified port and creates WearableConnection for each client.
 */
public class SocketConnectionThread extends Thread {
    private static final String TAG = "WearSocketThread";

    private final int port;
    private final ServerMessageListener listener;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private ServerSocket serverSocket;
    private WearableConnection connection;

    private SocketConnectionThread(int port, ServerMessageListener listener) {
        super("wear-socket-server");
        this.port = port;
        this.listener = listener;
        setDaemon(true);
    }

    /**
     * Start a server listening on the specified port.
     */
    public static SocketConnectionThread serverListen(int port, ServerMessageListener listener) {
        return new SocketConnectionThread(port, listener);
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            Log.i(TAG, "Wearable server listening on port " + port);

            while (running.get()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    Log.i(TAG, "Wearable device connected from " + clientSocket.getRemoteSocketAddress());

                    // Only allow one connection at a time
                    if (connection != null && connection.isConnected()) {
                        Log.w(TAG, "Closing previous connection");
                        connection.close();
                    }

                    connection = new WearableConnection(clientSocket, listener);
                    connection.start();

                } catch (IOException e) {
                    if (running.get()) {
                        Log.w(TAG, "Accept error", e);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Server socket error", e);
        } finally {
            close();
        }
    }

    public WearableConnection getWearableConnection() {
        return connection;
    }

    public void close() {
        running.set(false);
        if (connection != null) {
            connection.close();
        }
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            Log.w(TAG, "Error closing server socket", e);
        }
    }
}
