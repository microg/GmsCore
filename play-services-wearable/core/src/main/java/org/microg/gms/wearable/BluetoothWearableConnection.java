/*
 * SPDX-FileCopyrightText: 2024, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.squareup.wire.Wire;

import org.microg.wearable.WearableConnection;
import org.microg.wearable.proto.MessagePiece;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * WearableConnection that uses Bluetooth RFCOMM (SPP) transport.
 * This is the primary transport used by Wear OS watches for phone pairing.
 */
public class BluetoothWearableConnection extends WearableConnection {

    private static final String TAG = "GmsWearBtConn";
    private static final int MAX_PIECE_SIZE = 20 * 1024 * 1024;

    private final BluetoothSocket socket;
    private final DataInputStream is;
    private final DataOutputStream os;

    public BluetoothWearableConnection(BluetoothSocket socket, Listener listener) throws IOException {
        super(listener);
        this.socket = socket;
        this.is = new DataInputStream(socket.getInputStream());
        this.os = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    protected void writeMessagePiece(MessagePiece piece) throws IOException {
        byte[] bytes = piece.toByteArray();
        os.writeInt(bytes.length);
        os.write(bytes);
        os.flush();
    }

    @Override
    protected MessagePiece readMessagePiece() throws IOException {
        int len = is.readInt();
        if (len > MAX_PIECE_SIZE) {
            throw new IOException("Piece size " + len + " exceeded limit of " + MAX_PIECE_SIZE + " bytes.");
        }
        Log.d(TAG, "Reading piece of length " + len);
        byte[] bytes = new byte[len];
        is.readFully(bytes);
        return new Wire().parseFrom(bytes, MessagePiece.class);
    }

    @Override
    public void close() throws IOException {
        try {
            socket.close();
        } catch (IOException e) {
            Log.w(TAG, "Error closing Bluetooth socket", e);
        }
    }
}
