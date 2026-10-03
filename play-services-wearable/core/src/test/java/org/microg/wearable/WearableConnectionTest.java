/*
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.wearable;

import org.junit.Test;
import org.microg.wearable.proto.Heartbeat;
import org.microg.wearable.proto.MessagePiece;
import org.microg.wearable.proto.Request;
import org.microg.wearable.proto.RootMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okio.ByteString;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class WearableConnectionTest {
    private static final WearableConnection.Listener NOOP = new WearableConnection.Listener() {
        @Override public void onConnected(WearableConnection connection) {}
        @Override public void onMessage(WearableConnection connection, RootMessage message) {}
        @Override public void onDisconnected() {}
    };

    private static final class MemoryConnection extends WearableConnection {
        final List<MessagePiece> pieces = new ArrayList<>();
        int cursor;
        boolean closed;

        MemoryConnection() { super(NOOP); }
        MemoryConnection(WearableConnection.Listener listener) { super(listener); }

        @Override protected void writeMessagePiece(MessagePiece piece) { pieces.add(piece); }

        @Override protected MessagePiece readMessagePiece() throws IOException {
            if (cursor >= pieces.size()) throw new IOException("End of test stream");
            return pieces.get(cursor++);
        }

        @Override public void close() { closed = true; }
    }

    @Test public void roundTripsSinglePiece() throws IOException {
        MemoryConnection connection = new MemoryConnection();
        connection.writeMessage(new RootMessage.Builder().heartbeat(new Heartbeat()).build());
        assertEquals(1, connection.pieces.size());
        assertNotNull(connection.readMessage().heartbeat);
    }

    @Test public void roundTripsFragmentedMessage() throws IOException {
        MemoryConnection connection = new MemoryConnection();
        byte[] payload = new byte[150_000];
        for (int index = 0; index < payload.length; index++) payload[index] = (byte) index;
        RootMessage message = new RootMessage.Builder()
                .rpcRequest(new Request.Builder().rawData(ByteString.of(payload)).build()).build();
        connection.writeMessage(message);
        assertTrue(connection.pieces.size() > 1);
        assertEquals(ByteString.of(payload), connection.readMessage().rpcRequest.rawData);
    }

    @Test public void rejectsBadDigest() throws IOException {
        MemoryConnection connection = new MemoryConnection();
        connection.writeMessage(new RootMessage.Builder().heartbeat(new Heartbeat()).build());
        MessagePiece original = connection.pieces.get(0);
        connection.pieces.set(0, original.newBuilder().digest("invalid").build());
        assertThrows(IOException.class, connection::readMessage);
    }

    @Test public void rejectsInvalidCountWithoutAllocating() {
        MemoryConnection connection = new MemoryConnection();
        connection.pieces.add(new MessagePiece.Builder().data(ByteString.EMPTY).digest("invalid")
                .thisPiece(1).totalPieces(Integer.MAX_VALUE).queueId(1).build());
        assertThrows(IOException.class, connection::readMessage);
    }

    @Test public void rejectsOutOfOrderPieces() throws IOException {
        MemoryConnection connection = new MemoryConnection();
        connection.writeMessage(new RootMessage.Builder()
                .rpcRequest(new Request.Builder().rawData(ByteString.of(new byte[150_000])).build()).build());
        connection.pieces.remove(1);
        assertThrows(IOException.class, connection::readMessage);
    }

    @Test public void rejectsTooManyIncompleteQueues() {
        MemoryConnection connection = new MemoryConnection();
        for (int index = 0; index < 9; index++) {
            connection.pieces.add(new MessagePiece.Builder().data(ByteString.EMPTY).digest(WearableConnection.calculateDigest(new byte[0]))
                    .thisPiece(1).totalPieces(2).queueId(index).build());
        }
        assertThrows(IOException.class, connection::readMessage);
    }

    @Test public void closesTransportAfterMalformedInput() {
        MemoryConnection connection = new MemoryConnection();
        connection.pieces.add(new MessagePiece.Builder().build());
        connection.run();
        assertTrue(connection.closed);
    }

    @Test public void rejectsOversizedMetadata() {
        MemoryConnection connection = new MemoryConnection();
        connection.pieces.add(new MessagePiece.Builder().data(ByteString.EMPTY)
                .digest(new String(new char[100_000])).thisPiece(1).totalPieces(2).queueId(1).build());
        assertThrows(IOException.class, connection::readMessage);
    }

    @Test public void rejectsOversizedUnknownFields() {
        MemoryConnection connection = new MemoryConnection();
        MessagePiece.Builder piece = new MessagePiece.Builder().data(ByteString.EMPTY)
                .digest(WearableConnection.calculateDigest(new byte[0])).thisPiece(1).totalPieces(2).queueId(1);
        piece.addUnknownFields(ByteString.of(new byte[100_000]));
        connection.pieces.add(piece.build());
        assertThrows(IOException.class, connection::readMessage);
    }

    @Test public void failureDiagnosticsExcludePeerContentAndDigest() {
        List<String> diagnostic = new ArrayList<>();
        MemoryConnection connection = new MemoryConnection(new WearableConnection.Listener() {
            @Override public void onConnected(WearableConnection connection) { }
            @Override public void onMessage(WearableConnection connection, RootMessage message) { }
            @Override public void onDisconnected() { }
            @Override public void onConnectionError(String type, String location) {
                diagnostic.add(type + ":" + location);
            }
        });
        connection.pieces.add(new MessagePiece.Builder().data(ByteString.encodeUtf8("private-message"))
                .digest("private-digest").thisPiece(1).totalPieces(1).build());
        connection.run();
        assertTrue(connection.closed);
        assertEquals(2, diagnostic.size());
        assertTrue(diagnostic.get(0).contains("payloadBytes=15"));
        assertTrue(diagnostic.stream().noneMatch(value -> value.contains("private-message") || value.contains("private-digest")));
    }
}
