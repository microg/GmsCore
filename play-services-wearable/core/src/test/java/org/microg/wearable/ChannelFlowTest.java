/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.wearable;

import org.junit.Test;
import org.microg.wearable.proto.ChannelDataAckRequest;
import org.microg.wearable.proto.ChannelDataHeader;
import org.microg.wearable.proto.ChannelDataRequest;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okio.ByteString;

import static org.junit.Assert.*;

public class ChannelFlowTest {
    private ChannelFlow established() throws IOException {
        ChannelFlow flow = new ChannelFlow(42, true);
        flow.establish();
        return flow;
    }

    private ChannelDataHeader peerHeader(long sequence) {
        return new ChannelDataHeader.Builder().channelId(42L).fromChannelOperator(false)
                .requestId(sequence).build();
    }

    private ChannelDataRequest peerData(long sequence, boolean last) {
        return new ChannelDataRequest.Builder().header(peerHeader(sequence))
                .payload(ByteString.encodeUtf8("test")).finalMessage(last).build();
    }

    private ChannelDataAckRequest peerAck(long sequence, boolean last) {
        return new ChannelDataAckRequest.Builder().header(peerHeader(sequence)).finalMessage(last).build();
    }

    @Test public void refusesDataBeforeHandshake() {
        ChannelFlow flow = new ChannelFlow(42, true);
        assertThrows(IOException.class, () -> flow.send(new byte[0], false));
        assertThrows(IOException.class, () -> flow.receive(peerData(0, false)));
    }

    @Test public void receiverAcknowledgesOnlyAfterConsumption() throws Exception {
        ChannelFlow flow = established();
        ChannelDataRequest data = peerData(0, false);
        assertNull(flow.receive(data));
        assertSame(data, flow.awaitIncoming());
        ChannelDataAckRequest ack = flow.consumed(data);
        assertEquals(Long.valueOf(0), ack.header.requestId);
        assertTrue(ack.header.fromChannelOperator);
        assertSame(ack, flow.receive(data));
        assertNull(flow.receive(peerData(1, false)));
    }

    @Test public void pendingDuplicateDoesNotAllocateOrAcknowledgeAgain() throws Exception {
        ChannelFlow flow = established();
        ChannelDataRequest data = peerData(0, false);
        flow.receive(data);
        assertNull(flow.receive(peerData(0, false)));
        assertSame(data, flow.awaitIncoming());
        assertThrows(IOException.class, () -> flow.receive(data.newBuilder()
                .payload(ByteString.encodeUtf8("changed")).build()));
    }

    @Test public void refusesSecondPacketUntilFirstIsConsumed() throws Exception {
        ChannelFlow flow = established();
        flow.receive(peerData(0, false));
        assertThrows(IOException.class, () -> flow.receive(peerData(1, false)));
    }

    @Test public void boundsIncomingPayload() throws Exception {
        ChannelFlow flow = established();
        assertThrows(IOException.class, () -> flow.receive(peerData(0, false).newBuilder()
                .payload(ByteString.of(new byte[ChannelFlow.MAX_PAYLOAD + 1])).build()));
        assertNull(flow.receive(peerData(0, false).newBuilder()
                .payload(ByteString.of(new byte[ChannelFlow.MAX_PAYLOAD])).build()));
    }

    @Test public void rejectsWrongChannelRoleAndMissingHeader() throws Exception {
        ChannelFlow flow = established();
        assertThrows(IOException.class, () -> flow.receive(peerData(0, false).newBuilder()
                .header(peerHeader(0).newBuilder().channelId(43L).build()).build()));
        assertThrows(IOException.class, () -> flow.receive(peerData(0, false).newBuilder()
                .header(peerHeader(0).newBuilder().fromChannelOperator(true).build()).build()));
        assertThrows(IOException.class, () -> flow.receive(peerData(0, false).newBuilder().header(null).build()));
        assertThrows(IOException.class, () -> flow.receive(peerData(0, false).newBuilder().finalMessage(null).build()));
    }

    @Test public void senderWaitsForExactAcknowledgement() throws Exception {
        ChannelFlow flow = established();
        assertEquals(Long.valueOf(0), flow.send(new byte[]{1}, false).header.requestId);
        assertThrows(IOException.class, () -> flow.send(new byte[]{2}, false));
        assertThrows(IOException.class, () -> flow.acknowledge(peerAck(1, false)));
        assertThrows(IOException.class, () -> flow.acknowledge(peerAck(0, true)));
        flow.acknowledge(peerAck(0, false));
        flow.awaitAcknowledgement(1);
        assertEquals(Long.valueOf(1), flow.send(new byte[]{2}, false).header.requestId);
        flow.acknowledge(peerAck(0, false));
        assertThrows(IOException.class, () -> flow.awaitAcknowledgement(1));
    }

    @Test public void rejectsAckFromWrongRole() throws Exception {
        ChannelFlow flow = established();
        flow.send(new byte[0], false);
        assertThrows(IOException.class, () -> flow.acknowledge(peerAck(0, false).newBuilder()
                .header(peerHeader(0).newBuilder().fromChannelOperator(true).build()).build()));
    }

    @Test public void boundsUnknownFieldsInPacketHeaderAndAck() throws Exception {
        ChannelFlow flow = established();
        assertThrows(IOException.class, () -> flow.receive(peerData(0, false).newBuilder()
                .addUnknownFields(ByteString.of(new byte[ChannelFlow.MAX_PAYLOAD + 1025])).build()));
        assertThrows(IOException.class, () -> flow.receive(peerData(0, false).newBuilder()
                .header(peerHeader(0).newBuilder().addUnknownFields(ByteString.of(new byte[257])).build()).build()));
        flow.send(new byte[0], false);
        assertThrows(IOException.class, () -> flow.acknowledge(peerAck(0, false).newBuilder()
                .addUnknownFields(ByteString.of(new byte[1025])).build()));
    }

    @Test public void finalPacketEndsOnlyItsDirection() throws Exception {
        ChannelFlow flow = established();
        ChannelDataRequest last = peerData(0, true);
        flow.receive(last);
        ChannelDataAckRequest ack = flow.consumed(last);
        assertNull(flow.awaitIncoming());
        assertSame(ack, flow.receive(last));
        assertThrows(IOException.class, () -> flow.receive(peerData(1, false)));
        assertNotNull(flow.send(new byte[]{1}, false));
    }

    @Test public void cannotSendAfterFinalAck() throws Exception {
        ChannelFlow flow = established();
        flow.send(new byte[0], true);
        flow.acknowledge(peerAck(0, true));
        assertThrows(IOException.class, () -> flow.send(new byte[0], false));
    }

    @Test public void closeUnblocksReceiver() throws Exception {
        ChannelFlow flow = established();
        AtomicReference<Throwable> result = new AtomicReference<>();
        CountDownLatch entered = new CountDownLatch(1);
        Thread consumer = new Thread(() -> {
            entered.countDown();
            try { flow.awaitIncoming(); } catch (Throwable e) { result.set(e); }
        });
        consumer.start();
        assertTrue(entered.await(1, TimeUnit.SECONDS));
        flow.close();
        consumer.join(1000);
        assertFalse(consumer.isAlive());
        assertTrue(result.get() instanceof IOException);
        assertThrows(IOException.class, flow::establish);
    }

    @Test public void closeUnblocksSender() throws Exception {
        ChannelFlow flow = established();
        flow.send(new byte[0], false);
        AtomicReference<Throwable> result = new AtomicReference<>();
        Thread sender = new Thread(() -> {
            try { flow.awaitAcknowledgement(10000); } catch (Throwable e) { result.set(e); }
        });
        sender.start();
        flow.close();
        sender.join(1000);
        assertFalse(sender.isAlive());
        assertTrue(result.get() instanceof IOException);
    }

    @Test public void twoEndsExchangeDataInBothDirections() throws Exception {
        ChannelFlow a = established();
        ChannelFlow b = new ChannelFlow(42, false);
        b.establish();
        ChannelDataRequest toB = a.send(new byte[]{1, 2}, false);
        ChannelDataRequest toA = b.send(new byte[]{3, 4}, false);
        b.receive(toB);
        a.receive(toA);
        a.acknowledge(b.consumed(b.awaitIncoming()));
        b.acknowledge(a.consumed(a.awaitIncoming()));
        a.awaitAcknowledgement(1);
        b.awaitAcknowledgement(1);
        assertEquals(Long.valueOf(1), a.send(new byte[0], true).header.requestId);
    }
}
