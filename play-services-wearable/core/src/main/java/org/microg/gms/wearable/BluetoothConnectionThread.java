/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import org.microg.wearable.WearableConnection;
import org.microg.wearable.proto.MessagePiece;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/** A Bluetooth transport for the existing Wearable message protocol. */
final class BluetoothConnectionThread extends Thread {
    private static final String TAG = "GmsWearBluetooth";
    private static final UUID WEARABLE_SERVICE =
            UUID.fromString("5e8945b0-9525-11e3-a5e2-0800200c9a66");
    private static final int MAX_MESSAGE_PIECE_SIZE = 4 * 1024 * 1024;
    private static final long CONNECT_TIMEOUT_MS = 30000;

    private final BluetoothAdapter adapter;
    private final String address;
    private final WearableConnection.Listener listener;
    private volatile BluetoothSocket socket;
    private volatile boolean stopped;

    BluetoothConnectionThread(BluetoothAdapter adapter, String address,
                              WearableConnection.Listener listener) {
        super("Wearable Bluetooth " + address);
        this.adapter = adapter;
        this.address = address;
        this.listener = listener;
    }

    @Override
    public void run() {
        if (adapter == null) {
            Log.w(TAG, "Bluetooth is unavailable");
            return;
        }
        int retryDelayMs = 3000;
        while (!stopped && !isInterrupted()) {
            long connectedAt = 0;
            try {
                BluetoothDevice peer = adapter.getRemoteDevice(address);
                if (!adapter.isEnabled() || peer.getBondState() != BluetoothDevice.BOND_BONDED) {
                    throw new IOException("Paired Bluetooth device is unavailable");
                }
                BluetoothSocket candidate = peer.createRfcommSocketToServiceRecord(WEARABLE_SERVICE);
                socket = candidate;
                if (stopped || isInterrupted()) break;
                Timer timeout = new Timer("Wearable Bluetooth connect timeout", true);
                timeout.schedule(new TimerTask() {
                    @Override public void run() {
                        try {
                            candidate.close();
                        } catch (IOException ignored) {
                        }
                    }
                }, CONNECT_TIMEOUT_MS);
                try {
                    candidate.connect();
                } finally {
                    timeout.cancel();
                }
                Log.d(TAG, "Wearable Bluetooth socket connected");
                connectedAt = System.nanoTime();
                new BluetoothWearableConnection(candidate, listener).run();
            } catch (SecurityException | IllegalArgumentException e) {
                if (!stopped) Log.w(TAG, "Wearable Bluetooth permission or configuration is invalid");
                break;
            } catch (IOException e) {
                if (!stopped && !isInterrupted()) Log.w(TAG, "Unable to connect to the paired watch", e);
            } finally {
                BluetoothSocket current = socket;
                socket = null;
                if (current != null) {
                    try {
                        current.close();
                    } catch (IOException ignored) {
                    }
                }
            }
            if (stopped || isInterrupted()) break;
            if (connectedAt != 0 && System.nanoTime() - connectedAt >= 60_000_000_000L) retryDelayMs = 3000;
            try {
                Thread.sleep(retryDelayMs);
            } catch (InterruptedException e) {
                interrupt();
                break;
            }
            retryDelayMs = Math.min(retryDelayMs * 2, 30000);
        }
    }

    void closeConnection() {
        stopped = true;
        interrupt();
        BluetoothSocket current = socket;
        if (current != null) {
            try {
                current.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static final class BluetoothWearableConnection extends WearableConnection {
        private final BluetoothSocket socket;
        private final DataInputStream input;
        private final DataOutputStream output;

        BluetoothWearableConnection(BluetoothSocket socket, Listener listener) throws IOException {
            super(listener);
            this.socket = socket;
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
        }

        @Override
        protected void writeMessagePiece(MessagePiece piece) throws IOException {
            byte[] bytes = MessagePiece.ADAPTER.encode(piece);
            if (bytes.length == 0 || bytes.length > MAX_MESSAGE_PIECE_SIZE) {
                throw new IOException("Invalid outgoing message piece size");
            }
            synchronized (output) {
                output.writeInt(bytes.length);
                output.write(bytes);
                output.flush();
            }
        }

        @Override
        protected MessagePiece readMessagePiece() throws IOException {
            int size = input.readInt();
            if (size <= 0 || size > MAX_MESSAGE_PIECE_SIZE) {
                throw new IOException("Invalid incoming message piece size");
            }
            byte[] bytes = new byte[size];
            input.readFully(bytes);
            return MessagePiece.ADAPTER.decode(bytes);
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }
}
