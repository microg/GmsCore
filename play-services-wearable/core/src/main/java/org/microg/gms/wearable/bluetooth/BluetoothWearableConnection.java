/*
 * SPDX-FileCopyrightText: 2015, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.microg.gms.profile.Build;
import org.microg.gms.wearable.ConnectHandshake;
import org.microg.gms.wearable.WearableConnection;
import org.microg.gms.wearable.proto.Connect;
import org.microg.gms.wearable.proto.Heartbeat;
import org.microg.gms.wearable.proto.MessagePiece;
import org.microg.gms.wearable.proto.RootMessage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class BluetoothWearableConnection extends WearableConnection {
    private static final String TAG = "BtWearableConnection";
    private final int MAX_PIECE_SIZE = 64 * 1024 * 1024;
    private final BluetoothSocket socket;
    private final DataInputStream is;
    private final DataOutputStream os;
    private final Listener listener;

    private final String localNodeId;
    private String peerNodeId;
    private boolean handshakeComplete = false;
    private Connect peerConnect;

    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private volatile Thread readerThread;

    private final Handler watchdogHandler;
    private static final long READ_TIMEOUT_MS = 60000;
    private static final long HANDSHAKE_TIMEOUT_MS = 30000;

    private static final long HEARTBEAT_INTERVAL_MS = 20000;
    private volatile boolean heartbeatEnabled = false;
    private Thread heartbeatThread;
    private final long androidId;

    public BluetoothWearableConnection(BluetoothSocket socket, String localNodeId, long androidId, Listener listener) throws IOException {
        super(listener);
        this.socket = socket;
        this.is = new DataInputStream(socket.getInputStream());
        this.os = new DataOutputStream(socket.getOutputStream());
        this.localNodeId = localNodeId;
        this.listener = listener;
        this.watchdogHandler = new Handler(Looper.getMainLooper());
        this.androidId = androidId;

        if (localNodeId == null) {
            throw new IllegalArgumentException("localNodeId cannot be null");
        }
    }

    public boolean handshake() {
        Log.d(TAG, "Starting handshake, local node ID: " + localNodeId);

        final AtomicBoolean timedOut = new AtomicBoolean(false);

        Runnable timeoutWatchdog = () -> {
            if (!handshakeComplete) {
                Log.e(TAG, "Handshake timeout after " + HANDSHAKE_TIMEOUT_MS + "ms - forcing close");
                timedOut.set(true);
                try {
                    socket.close();
                } catch (IOException e) {
                    Log.w(TAG, "Error closing socket on timeout", e);
                }
            }
        };

        watchdogHandler.postDelayed(timeoutWatchdog, HANDSHAKE_TIMEOUT_MS);

        try {
            Connect peer = ConnectHandshake.perform(this, localNodeId, Build.MODEL, androidId);

            if (timedOut.get()) {
                return false;
            }
            this.peerNodeId = peer.id;
            handshakeComplete = true;
            return true;

        } catch (IOException e) {
            if (timedOut.get()) {
                Log.e(TAG, "Handshake failed due to timeout", e);
            } else {
                Log.e(TAG, "Handshake failed", e);
            }
            return false;
        } finally {
            watchdogHandler.removeCallbacks(timeoutWatchdog);
        }
    }

    public String getPeerNodeId() {
        return peerNodeId;
    }

    public String getLocalNodeId() {
        return localNodeId;
    }

    public boolean isHandshakeComplete() {
        return handshakeComplete;
    }

    protected void writeMessagePiece(MessagePiece piece) throws IOException {
        if (isClosed.get()) {
            throw new IOException("Socket not connected");
        }

        byte[] bytes = MessagePiece.ADAPTER.encode(piece);

        synchronized (os) {
            try {
                os.writeInt(bytes.length);
                os.write(bytes);
                os.flush();
            } catch (IOException e) {
                Log.e(TAG, "Write failed, socket may be closed", e);
                throw e;
            }
        }
    }

    @Override
    public void run() {
        readerThread = Thread.currentThread();

        startHeartbeat();

        try {
            // Perform handshake first
            if (!handshake()) {
                Log.e(TAG, "Handshake failed, closing connection");
                try {
                    close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing connection after handshake failure", e);
                }
                return;
            }

            super.run();

        } catch (Exception e) {
            Log.e(TAG, "Error in connection run loop", e);
        } finally {
            stopHeartbeat();
            readerThread = null;
        }
    }

    private void startHeartbeat() {
        heartbeatEnabled = true;
        heartbeatThread = new Thread(() -> {
            Log.d(TAG, "Heartbeat thread started for peer: " + peerNodeId);
            while (heartbeatEnabled && !isClosed()) {
                try {
                    Thread.sleep(HEARTBEAT_INTERVAL_MS);

                    if (heartbeatEnabled && !isClosed()) {
                        Log.d(TAG, "Sending heartbeat to " + peerNodeId);
                        writeMessage(new RootMessage.Builder()
                                .heartbeat(new Heartbeat())
                                .build());
                    }
                } catch (InterruptedException e) {
                    Log.d(TAG, "Heartbeat thread interrupted");
                    break;
                } catch (IOException e) {
                    Log.w(TAG, "Failed to send heartbeat, closing connection", e);
                    try {
                        close();
                    } catch (IOException ex) {
                        Log.w(TAG, "Error closing connection", ex);
                    }
                    break;
                }
            }
            Log.d(TAG, "Heartbeat thread stopped for peer: " + peerNodeId);
        }, "BtHeartbeat-" + peerNodeId);

        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }

    private void stopHeartbeat() {
        heartbeatEnabled = false;
        if (heartbeatThread != null) {
            heartbeatThread.interrupt();
            try {
                heartbeatThread.join(1000);
            } catch (InterruptedException e) {
                // Ignore
            }
            heartbeatThread = null;
        }
    }

    protected MessagePiece readMessagePiece() throws IOException {
        if (isClosed.get()) {
            throw new IOException("Socket not connected");
        }

        final AtomicBoolean timedOut = new AtomicBoolean(false);
        final Thread currentThread = Thread.currentThread();

        Runnable readWatchdog = () -> {
            Log.e(TAG, "Read operation timed out after " + READ_TIMEOUT_MS + "ms");
            timedOut.set(true);
            try {
                socket.close();
            } catch (IOException e) {
                Log.w(TAG, "Error closing socket on read timeout", e);
            }
            currentThread.interrupt();
        };

        watchdogHandler.postDelayed(readWatchdog, READ_TIMEOUT_MS);

        try {
            int len = is.readInt();

            if (len <= 0 || len > MAX_PIECE_SIZE) {
                throw new IOException("Invalid piece length: " + len);
            }

            byte[] bytes = new byte[len];

            try {
                is.readFully(bytes);
            } catch (EOFException e) {
                throw new IOException("Socket closed by peer while reading data", e);
            }

            watchdogHandler.removeCallbacks(readWatchdog);
            return MessagePiece.ADAPTER.decode(bytes);
        } catch (IOException e) {
            watchdogHandler.removeCallbacks(readWatchdog);

            if (isClosed.get()) {
                throw new IOException("Connection closed during read", e);
            }

            String msg = e.getMessage();
            if (msg != null && msg.contains("bt socket closed")) {
                Log.d(TAG, "Bluetooth socket closed during read");
                isClosed.set(true);
            }

            throw new IOException("Connection closed by peer", e);
        }
    }

    @Override
    public void close() throws IOException {
        if (isClosed.getAndSet(true)) {
            Log.d(TAG, "Connection already closed");
            return;
        }

        Log.d(TAG, "Closing Bluetooth wearable connection");

        stopHeartbeat();

        Thread reader = readerThread;
        if (reader != null && reader != Thread.currentThread()) {
            reader.interrupt();
        }

        IOException exception = null;

        try {
            if (is != null) {
                is.close();
            }
        } catch (IOException e) {
            Log.w(TAG, "Error closing input stream", e);
            exception = e;
        }

        try {
            if (os != null) {
                os.close();
            }
        } catch (IOException e) {
            Log.w(TAG, "Error closing output stream", e);
            if (exception == null) exception = e;
        }

        try {
            socket.close();
        } catch (IOException e) {
            Log.w(TAG, "Error closing socket", e);
            if (exception == null) exception = e;
        }

        if (exception != null) {
            throw exception;
        }
    }

    public Connect getPeerConnect() {
        return peerConnect;
    }

    public boolean isClosed() {
        return isClosed.get();
    }

}