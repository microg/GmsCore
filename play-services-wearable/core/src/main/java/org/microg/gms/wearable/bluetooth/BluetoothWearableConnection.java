/*
 * SPDX-FileCopyrightText: 2015, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import org.microg.gms.profile.Build;
import org.microg.gms.wearable.ConnectHandshake;
import org.microg.gms.wearable.WearableConnection;
import org.microg.gms.wearable.proto.Connect;
import org.microg.gms.wearable.proto.MessagePiece;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
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

    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private volatile Thread readerThread;

    private final HandlerThread watchdogThread;
    private final Handler watchdogHandler;
    private static final long HANDSHAKE_TIMEOUT_MS = 30000;
    private static final long WRITE_STUCK_TIMEOUT_MS = 15000;
    private final Runnable writeStuckRunnable;

    private final long androidId;

    public BluetoothWearableConnection(BluetoothSocket socket, String localNodeId, long androidId, Listener listener) throws IOException {
        super(listener);
        this.socket = socket;
        this.is = new DataInputStream(socket.getInputStream());
        this.os = new DataOutputStream(socket.getOutputStream());
        this.localNodeId = localNodeId;
        this.listener = listener;
        this.androidId = androidId;

        if (localNodeId == null) {
            throw new IllegalArgumentException("localNodeId cannot be null");
        }
        this.watchdogThread = new HandlerThread("BtWatchdog-" + localNodeId);
        this.watchdogThread.start();
        this.watchdogHandler = new Handler(watchdogThread.getLooper());

        this.writeStuckRunnable = () -> {
            if (isClosed.get())
                return;

            Log.e(TAG, "Write stuck for more than " + WRITE_STUCK_TIMEOUT_MS
                    + "ms - closing socket");
            isClosed.set(true);
            try {
                socket.close();
            } catch (IOException e) {
                Log.w(TAG, "Error closing socket on write-stuck", e);
            }
        };
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
            setPeerConnect(peer);
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
        byte[] bytes = MessagePiece.ADAPTER.encode(piece);

        synchronized (os) {
            if (isClosed.get()) {
                throw new IOException("Socket not connected");
            }
            watchdogHandler.postDelayed(writeStuckRunnable, WRITE_STUCK_TIMEOUT_MS);

            try {
                os.writeInt(bytes.length);
                os.write(bytes);
                os.flush();
            } catch (IOException e) {
                Log.e(TAG, "Write failed, socket may be closed", e);
                throw e;
            } finally {
                watchdogHandler.removeCallbacks(writeStuckRunnable);
            }
        }
    }

    @Override
    public void run() {
        readerThread = Thread.currentThread();

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
            readerThread = null;
        }
    }

    protected MessagePiece readMessagePiece() throws IOException {
        if (isClosed.get()) {
            throw new IOException("Socket not connected");
        }

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

            return MessagePiece.ADAPTER.decode(bytes);
        } catch (IOException e) {
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

        watchdogHandler.removeCallbacksAndMessages(null);
        watchdogThread.quitSafely();

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

    public boolean isClosed() {
        return isClosed.get();
    }

    @Override
    public String getRemoteAddress() {
        return socket.getRemoteDevice().getAddress();
    }
}