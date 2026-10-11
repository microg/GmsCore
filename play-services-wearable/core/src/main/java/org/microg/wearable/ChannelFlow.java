/*
 * SPDX-FileCopyrightText: 2026 microG contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.wearable;

import org.microg.wearable.proto.ChannelDataAckRequest;
import org.microg.wearable.proto.ChannelDataHeader;
import org.microg.wearable.proto.ChannelDataRequest;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okio.ByteString;

/** One bounded, stop-and-wait stream in each direction of a channel. */
public final class ChannelFlow {
    public static final int MAX_PAYLOAD = 65536;
    private final long id;
    private final boolean opener;
    private boolean established;
    private boolean closed;
    private boolean incomingEnded;
    private boolean outgoingEnded;
    private long incomingId;
    private long outgoingId;
    private ChannelDataRequest incoming;
    private ChannelDataRequest outgoing;
    private ChannelDataAckRequest lastIncomingAck;

    public ChannelFlow(long id, boolean opener) {
        this.id = id;
        this.opener = opener;
    }

    public synchronized void establish() throws IOException {
        requireLive();
        established = true;
        notifyAll();
    }

    private void requireLive() throws IOException {
        if (closed) throw new IOException("Channel is closed");
    }

    private void requireEstablished() throws IOException {
        requireLive();
        if (!established) throw new IOException("Channel has not been acknowledged");
    }

    private void validatePeer(ChannelDataHeader header) throws IOException {
        if (header == null || header.channelId == null || header.channelId != id
                || header.fromChannelOperator == null || header.fromChannelOperator == opener
                || header.requestId == null || header.requestId < 0) {
            throw new IOException("Invalid channel header");
        }
        if (ChannelDataHeader.ADAPTER.encodedSize(header) > 256) {
            throw new IOException("Channel header exceeds metadata limit");
        }
    }

    /** Returns the ACK of an already consumed duplicate; pending duplicates remain unacknowledged. */
    public synchronized ChannelDataAckRequest receive(ChannelDataRequest data) throws IOException {
        requireEstablished();
        validatePeer(data.header);
        if (data.payload == null || data.payload.size() > MAX_PAYLOAD || data.finalMessage == null) {
            throw new IOException("Invalid channel payload");
        }
        if (ChannelDataRequest.ADAPTER.encodedSize(data) > MAX_PAYLOAD + 1024) {
            throw new IOException("Channel packet exceeds metadata limit");
        }
        long sequence = data.header.requestId;
        if (sequence == incomingId - 1 && lastIncomingAck != null) return lastIncomingAck;
        if (sequence != incomingId || incomingEnded) throw new IOException("Out-of-order channel data");
        if (incoming != null) {
            if (!incoming.equals(data)) throw new IOException("Changed pending channel data");
            return null;
        }
        incoming = data;
        notifyAll();
        return null;
    }

    public synchronized ChannelDataRequest awaitIncoming() throws IOException, InterruptedException {
        requireEstablished();
        while (incoming == null && !incomingEnded && !closed) wait();
        requireLive();
        return incoming;
    }

    /** Called only after the bytes have been written to the local consumer's pipe. */
    public synchronized ChannelDataAckRequest consumed(ChannelDataRequest data) throws IOException {
        requireEstablished();
        if (incoming != data) throw new IOException("Channel packet is not pending");
        lastIncomingAck = new ChannelDataAckRequest.Builder()
                .header(header(incomingId++)) .finalMessage(data.finalMessage).build();
        incomingEnded = data.finalMessage;
        incoming = null;
        return lastIncomingAck;
    }

    public synchronized ChannelDataRequest send(byte[] payload, boolean last) throws IOException {
        requireEstablished();
        if (payload == null || payload.length > MAX_PAYLOAD || outgoing != null || outgoingEnded) {
            throw new IOException("Channel sender is not ready");
        }
        outgoing = new ChannelDataRequest.Builder().header(header(outgoingId))
                .payload(ByteString.of(payload)).finalMessage(last).build();
        return outgoing;
    }

    public synchronized void acknowledge(ChannelDataAckRequest ack) throws IOException {
        requireEstablished();
        validatePeer(ack.header);
        if (ack.finalMessage == null) throw new IOException("Invalid channel acknowledgement");
        if (ChannelDataAckRequest.ADAPTER.encodedSize(ack) > 1024) {
            throw new IOException("Channel acknowledgement exceeds metadata limit");
        }
        if (ack.header.requestId < outgoingId) return; // Retransmitted ACK.
        if (outgoing == null || ack.header.requestId != outgoingId
                || !ack.finalMessage.equals(outgoing.finalMessage)) {
            throw new IOException("Unexpected channel acknowledgement");
        }
        outgoingEnded = outgoing.finalMessage;
        outgoing = null;
        outgoingId++;
        notifyAll();
    }

    public synchronized void awaitAcknowledgement(long timeoutMillis) throws IOException, InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        while (outgoing != null && !closed) {
            long left = deadline - System.nanoTime();
            if (left <= 0) throw new IOException("Channel acknowledgement timed out");
            TimeUnit.NANOSECONDS.timedWait(this, left);
        }
        requireLive();
    }

    private ChannelDataHeader header(long sequence) {
        return new ChannelDataHeader.Builder().channelId(id).fromChannelOperator(opener)
                .requestId(sequence).build();
    }

    public synchronized void close() {
        closed = true;
        incoming = null;
        outgoing = null;
        notifyAll();
    }
}
