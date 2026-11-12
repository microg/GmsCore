/*
 * Copyright (C) 2013-2019 microG Project Team
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

package org.microg.gms.wearable;

import android.bluetooth.BluetoothSocket;

import org.microg.wearable.WearableConnection;
import org.microg.wearable.proto.MessagePiece;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Bluetooth transport implementation for Wearable connections.
 * Uses RFCOMM sockets to communicate with WearOS devices over Bluetooth Classic.
 */
public class BluetoothWearableConnection extends WearableConnection {
    private final int MAX_PIECE_SIZE = 20 * 1024 * 1024; // 20MB limit
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
        byte[] bytes = piece.encode();
        os.writeInt(bytes.length);
        os.write(bytes);
        os.flush();
    }

    @Override
    protected MessagePiece readMessagePiece() throws IOException {
        int len = is.readInt();
        if (len > MAX_PIECE_SIZE || len < 0) {
            throw new IOException("Piece size " + len + " exceeded limit of " + MAX_PIECE_SIZE + " bytes.");
        }
        byte[] bytes = new byte[len];
        is.readFully(bytes);
        
        // Use reflection to call wire.parseFrom() to work around Wire version incompatibility
        // The inherited 'wire' instance from WearableConnection uses Wire 1.6.1 API
        try {
            Method parseFrom = wire.getClass().getMethod("parseFrom", byte[].class, Class.class);
            return (MessagePiece) parseFrom.invoke(wire, bytes, MessagePiece.class);
        } catch (Exception e) {
            throw new IOException("Failed to deserialize MessagePiece: " + e.getMessage(), e);
        }
    }

    public String getRemoteAddress() {
        return socket.getRemoteDevice().getAddress();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
