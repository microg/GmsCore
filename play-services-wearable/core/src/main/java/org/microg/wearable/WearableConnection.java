/*
 * SPDX-FileCopyrightText: 2015, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.wearable;


import org.microg.wearable.proto.MessagePiece;
import org.microg.wearable.proto.RootMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okio.ByteString;

public abstract class WearableConnection implements Runnable {
    private static final String B64ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
    private static final int MAX_MESSAGE_SIZE = 16 * 1024 * 1024;
    private static final int PIECE_SIZE = 64 * 1024;
    private static final int MAX_QUEUES = 8;

    private final HashMap<Integer, List<MessagePiece>> piecesQueues = new HashMap<>();
    private final Listener listener;
    private int nextQueueId;
    private int bufferedBytes;

    public WearableConnection(Listener listener) {
        this.listener = listener;
    }

    public static String base64encode(byte[] bytes) {
        int paddingCount = (3 - (bytes.length % 3)) % 3;
        byte[] padded = new byte[bytes.length + paddingCount];
        System.arraycopy(bytes, 0, padded, 0, bytes.length);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i += 3) {
            int j = ((padded[i] & 0xff) << 16) + ((padded[i + 1] & 0xff) << 8) + (padded[i + 2] & 0xff);
            sb.append(B64ALPHABET.charAt((j >> 18) & 0x3f)).append(B64ALPHABET.charAt((j >> 12) & 0x3f))
                    .append(B64ALPHABET.charAt((j >> 6) & 0x3f)).append(B64ALPHABET.charAt(j & 0x3f));
        }
        return sb.substring(0, sb.length() - paddingCount);
    }

    public static String calculateDigest(byte[] bytes) {
        try {
            return base64encode(MessageDigest.getInstance("SHA1").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA1 not supported => platform not supported");
        }
    }

    public synchronized void writeMessage(RootMessage message) throws IOException {
        byte[] bytes = RootMessage.ADAPTER.encode(message);
        if (bytes.length > MAX_MESSAGE_SIZE) throw new IOException("Wearable message too large");
        int total = Math.max(1, (bytes.length + PIECE_SIZE - 1) / PIECE_SIZE);
        int queueId = ++nextQueueId;
        String digest = calculateDigest(bytes);
        for (int index = 0; index < total; index++) {
            int offset = index * PIECE_SIZE;
            int length = Math.min(PIECE_SIZE, bytes.length - offset);
            writeMessagePiece(new MessagePiece.Builder()
                    .data(ByteString.of(bytes, offset, length))
                    .digest(digest)
                    .thisPiece(index + 1)
                    .totalPieces(total)
                    .queueId(queueId).build());
        }
    }

    protected abstract void writeMessagePiece(MessagePiece piece) throws IOException;

    protected RootMessage readMessage() throws IOException {
        while (true) {
            MessagePiece piece = readMessagePiece();
            if (piece == null || piece.data == null || piece.digest == null || piece.digest.length() != 27
                    || piece.totalPieces == null || piece.thisPiece == null
                    || piece.totalPieces < 1 || piece.totalPieces > MAX_MESSAGE_SIZE / PIECE_SIZE
                    || piece.thisPiece < 1 || piece.thisPiece > piece.totalPieces
                    || piece.data.size() > PIECE_SIZE
                    || MessagePiece.ADAPTER.encodedSize(piece) > PIECE_SIZE + 1024) {
                listener.onConnectionError("InvalidFrame", piece == null ? "null frame"
                        : "part=" + piece.thisPiece + ", total=" + piece.totalPieces
                        + ", digestLength=" + (piece.digest == null ? -1 : piece.digest.length())
                        + ", payloadBytes=" + (piece.data == null ? -1 : piece.data.size()));
                throw new IOException("Invalid wearable message piece");
            }
            if (piece.totalPieces == 1) {
                byte[] bytes = piece.data.toByteArray();
                if (!calculateDigest(bytes).equals(piece.digest)) throw new IOException("Invalid wearable message digest");
                return RootMessage.ADAPTER.decode(bytes);
            } else {
                if (piece.queueId == null) throw new IOException("Missing wearable message queue");
                int retainedSize = MessagePiece.ADAPTER.encodedSize(piece);
                if (bufferedBytes > MAX_MESSAGE_SIZE + 1024 * 1024 - retainedSize) {
                    throw new IOException("Wearable message buffers are full");
                }
                if (piece.thisPiece == 1) {
                    if (piecesQueues.containsKey(piece.queueId) || piecesQueues.size() >= MAX_QUEUES) {
                        throw new IOException("Too many or duplicate wearable message queues");
                    }
                    List<MessagePiece> queue = new ArrayList<>(piece.totalPieces);
                    queue.add(piece);
                    bufferedBytes += retainedSize;
                    piecesQueues.put(piece.queueId, queue);
                } else {
                    List<MessagePiece> queue = piecesQueues.get(piece.queueId);
                    if (queue == null || !queue.get(0).digest.equals(piece.digest)
                            || !queue.get(0).totalPieces.equals(piece.totalPieces)) {
                        throw new IOException("Invalid wearable message sequence");
                    }
                    if (queue.size() + 1 != piece.thisPiece) {
                        throw new IOException("Out of order wearable message piece");
                    }
                    queue.add(piece);
                    bufferedBytes += retainedSize;
                    if (piece.thisPiece.equals(piece.totalPieces)) {
                        piecesQueues.remove(piece.queueId);
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        for (MessagePiece messagePiece : queue) {
                            bufferedBytes -= MessagePiece.ADAPTER.encodedSize(messagePiece);
                            if (bos.size() + messagePiece.data.size() > MAX_MESSAGE_SIZE) {
                                throw new IOException("Wearable message too large");
                            }
                            messagePiece.data.write(bos);
                        }
                        byte[] bytes = bos.toByteArray();
                        if (!calculateDigest(bytes).equals(piece.digest)) {
                            throw new IOException("Invalid wearable message digest");
                        }
                        return RootMessage.ADAPTER.decode(bytes);
                    }
                }
            }
        }
    }

    protected abstract MessagePiece readMessagePiece() throws IOException;

    public abstract void close() throws IOException;

    @Override
    public void run() {
        try {
            listener.onConnected(this);
            RootMessage message;
            while ((message = readMessage()) != null) {
                listener.onMessage(this, message);
            }
        } catch (IOException | RuntimeException e) {
            // Report the failure location, never the exception message or peer payload.
            StackTraceElement[] trace = e.getStackTrace();
            listener.onConnectionError(e.getClass().getSimpleName(), trace.length == 0 ? "unknown"
                    : trace[0].getClassName() + "." + trace[0].getMethodName() + ":" + trace[0].getLineNumber());
        } finally {
            piecesQueues.clear();
            bufferedBytes = 0;
            try {
                close();
            } catch (IOException ignored) {
            }
            listener.onDisconnected();
        }
    }

    public interface Listener {
        void onConnected(WearableConnection connection);
        void onMessage(WearableConnection connection, RootMessage message);
        void onDisconnected();
        default void onConnectionError(String errorType, String location) { }
    }
}
