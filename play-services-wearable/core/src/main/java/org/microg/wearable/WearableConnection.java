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

import org.microg.wearable.proto.RootMessage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages a single wearable connection over TCP socket.
 * Handles reading/writing of RootMessage protocol.
 */
public class WearableConnection {
    private static final String TAG = "WearConnection";

    private final Socket socket;
    private final DataInputStream input;
    private final DataOutputStream output;
    private final ServerMessageListener listener;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final BlockingQueue<RootMessage> writeQueue = new LinkedBlockingQueue<>();
    private Thread readThread;
    private Thread writeThread;

    public WearableConnection(Socket socket, ServerMessageListener listener) throws IOException {
        this.socket = socket;
        this.input = new DataInputStream(socket.getInputStream());
        this.output = new DataOutputStream(socket.getOutputStream());
        this.listener = listener;
    }

    public void start() {
        readThread = new Thread(this::readLoop, "wear-read");
        readThread.setDaemon(true);
        readThread.start();

        writeThread = new Thread(this::writeLoop, "wear-write");
        writeThread.setDaemon(true);
        writeThread.start();
    }

    public void writeMessage(RootMessage message) {
        if (running.get()) {
            writeQueue.offer(message);
        }
    }

    public void close() {
        running.set(false);
        try {
            socket.close();
        } catch (IOException e) {
            Log.w(TAG, "Error closing socket", e);
        }
        writeQueue.clear();
    }

    public boolean isConnected() {
        return running.get() && !socket.isClosed();
    }

    private void readLoop() {
        try {
            while (running.get() && !socket.isClosed()) {
                int length = input.readInt();
                if (length <= 0 || length > 10 * 1024 * 1024) { // Max 10MB
                    Log.w(TAG, "Invalid message length: " + length);
                    break;
                }

                byte[] data = new byte[length];
                input.readFully(data);

                RootMessage message = RootMessage.ADAPTER.decode(data);
                if (message != null) {
                    listener.onMessageReceived(this, message);
                }
            }
        } catch (IOException e) {
            if (running.get()) {
                Log.w(TAG, "Read error", e);
            }
        } finally {
            running.set(false);
            listener.onConnectionClosed(this);
        }
    }

    private void writeLoop() {
        try {
            while (running.get() && !socket.isClosed()) {
                RootMessage message = writeQueue.take();
                byte[] data = RootMessage.ADAPTER.encode(message);
                output.writeInt(data.length);
                output.write(data);
                output.flush();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            if (running.get()) {
                Log.w(TAG, "Write error", e);
            }
        } finally {
            running.set(false);
        }
    }
}
