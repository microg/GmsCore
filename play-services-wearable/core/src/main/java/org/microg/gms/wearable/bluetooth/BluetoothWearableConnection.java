/*
 * SPDX-FileCopyrightText: 2015, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import org.microg.gms.profile.Build;
import org.microg.gms.wearable.WearableConnection;
import org.microg.gms.wearable.proto.Connect;
import org.microg.gms.wearable.proto.MessagePiece;
import org.microg.gms.wearable.proto.RootMessage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class BluetoothWearableConnection extends WearableConnection {
    private static final String TAG = "BtWearableConnection";
    private final int MAX_PIECE_SIZE = 20 * 1024 * 1024;
    private final BluetoothSocket socket;
    private final DataInputStream is;
    private final DataOutputStream os;
    private final Listener listener;

    private final String localNodeId;
    private String peerNodeId;
    private boolean handshakeComplete = false;
    private Connect peerConnect;

    public BluetoothWearableConnection(BluetoothSocket socket, String localNodeId, Listener listener) throws IOException {
        super(listener);
        this.socket = socket;
        this.is = new DataInputStream(socket.getInputStream());
        this.os = new DataOutputStream(socket.getOutputStream());
        this.localNodeId = localNodeId;
        this.listener = listener;

        if (localNodeId == null) {
            throw new IllegalArgumentException("localNodeId cannot be null");
        }
    }

    private boolean handshake() {
        try {
            Log.d(TAG, "Starting handshake, local node ID: " + localNodeId);

            Connect connectMessage = new Connect.Builder()
                    .id(localNodeId)
                    .name(Build.MODEL)
                    .peerVersion(2)
                    .peerMinimumVersion(0)
                    .build();

            RootMessage outgoingMessage = new RootMessage.Builder()
                    .connect(connectMessage)
                    .build();

            writeMessage(outgoingMessage);
            Log.d(TAG, "Sent Connect message with node ID: " + localNodeId);

            RootMessage incomingMessage = readMessage();
            Log.d(TAG, "Received message type: " + incomingMessage);

            if (incomingMessage.connect == null) {
                Log.e(TAG, "Expected Connect message but received: " + incomingMessage);
                return false;
            }

            this.peerConnect = incomingMessage.connect;
            this.peerNodeId = peerConnect.id;

            if (peerNodeId == null || peerNodeId.isEmpty()) {
                Log.e(TAG, "Received invalid peer node ID");
                return false;
            }

            Log.d(TAG, "Handshake successful! Peer node ID: " + peerNodeId);
            Log.d(TAG, "Connect message details: " + incomingMessage.connect);

            handshakeComplete = true;
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Handshake failed", e);
            return false;
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
//        byte[] bytes = piece.toByteArray();
        byte[] bytes = MessagePiece.ADAPTER.encode(piece);
        os.writeInt(bytes.length);
        os.write(bytes);
        os.flush();
    }

    @Override
    public void run() {
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
        }
    }

    protected MessagePiece readMessagePiece() throws IOException {
        int len = is.readInt();
        if (len > MAX_PIECE_SIZE) {
            throw new IOException("Piece size " + len + " exceeded limit of " + MAX_PIECE_SIZE + " bytes.");
        }
        System.out.println("Reading piece of length " + len);
        byte[] bytes = new byte[len];
        is.readFully(bytes);
//        return wire.parseFrom(bytes, MessagePiece.class);
        return MessagePiece.ADAPTER.decode(bytes);
    }

    @Override
    public void close() throws IOException {
        try {
            if (is != null) is.close();
        } catch (IOException e) {
            // Ignore
        }
        try {
            if (os != null) os.close();
        } catch (IOException e) {
            // Ignore
        }
        socket.close();
    }

    public Connect getPeerConnect() {
        return peerConnect;
    }
}