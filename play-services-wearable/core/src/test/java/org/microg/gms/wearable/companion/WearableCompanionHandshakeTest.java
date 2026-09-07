/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable.companion;

import junit.framework.TestCase;

import org.junit.Assert;
import org.microg.gms.wearable.notification.AncsNotifications;
import org.microg.wearable.ServerMessageListener;
import org.microg.wearable.SocketConnectionThread;
import org.microg.wearable.WearableConnection;
import org.microg.wearable.proto.Connect;
import org.microg.wearable.proto.SetDataItem;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Complementary-handshake verification over a real loopback transport, without wearable
 * hardware. Both ends run the same {@link SocketConnectionThread} server/client code the
 * companion link uses (int32 length prefix + MessagePiece protobuf framing), so the exercised
 * bytes are identical to a TCP companion session. Bluetooth RFCOMM wraps the same connection.
 */
public class WearableCompanionHandshakeTest extends TestCase {

    private static byte[] ancsFrame(long uid, String app, String title) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(AncsNotifications.COMMAND_GET_NOTIFICATION_ATTRIBUTES);
        out.write((int) (uid & 0xFF));
        out.write((int) ((uid >>> 8) & 0xFF));
        out.write((int) ((uid >>> 16) & 0xFF));
        out.write((int) ((uid >>> 24) & 0xFF));
        append(AncsNotifications.ATTR_APP_IDENTIFIER,
                (app + "\0").getBytes(java.nio.charset.StandardCharsets.UTF_8), out);
        append(AncsNotifications.ATTR_TITLE, title.getBytes(java.nio.charset.StandardCharsets.UTF_8), out);
        return out.toByteArray();
    }

    private static void append(int attributeId, byte[] value, ByteArrayOutputStream out) {
        out.write(attributeId);
        out.write(value.length & 0xFF);
        out.write((value.length >>> 8) & 0xFF);
        out.write(value, 0, value.length);
    }

    /**
     * One end of the companion link: echoes its {@link Connect} on connect (standard
     * {@link ServerMessageListener} behaviour) and collects the peer {@link Connect} plus any
     * {@link SetDataItem} messages it receives.
     */
    private static class CompanionEndpoint extends ServerMessageListener {
        final CountDownLatch connected = new CountDownLatch(1);
        final CountDownLatch gotPeerConnect = new CountDownLatch(1);
        final CountDownLatch gotDataItem = new CountDownLatch(1);
        volatile SetDataItem dataItem;
        volatile WearableConnection connection;

        CompanionEndpoint(String id, String name) {
            super(new Connect.Builder()
                    .id(id)
                    .name(name)
                    .peerAndroidId(0x123456789abcdefL)
                    .peerVersion(1)
                    .peerMinimumVersion(1)
                    .build());
        }

        @Override
        public void onConnected(WearableConnection connection) {
            super.onConnected(connection);
            this.connection = connection;
            connected.countDown();
        }

        @Override
        public void onConnect(Connect connect) {
            super.onConnect(connect);
            gotPeerConnect.countDown();
        }

        @Override
        public void onSetDataItem(SetDataItem item) {
            this.dataItem = item;
            gotDataItem.countDown();
        }

        @Override
        public void onSetAsset(org.microg.wearable.proto.SetAsset message) {
        }

        @Override
        public void onAckAsset(org.microg.wearable.proto.AckAsset message) {
        }

        @Override
        public void onFetchAsset(org.microg.wearable.proto.FetchAsset message) {
        }

        @Override
        public void onSyncStart(org.microg.wearable.proto.SyncStart message) {
        }

        @Override
        public void onRpcRequest(org.microg.wearable.proto.Request message) {
        }

        @Override
        public void onHeartbeat(org.microg.wearable.proto.Heartbeat message) {
        }

        @Override
        public void onFilePiece(org.microg.wearable.proto.FilePiece message) {
        }

        @Override
        public void onChannelRequest(org.microg.wearable.proto.Request message) {
        }
    }

    public void testHandshakeAndNotificationRoundTripOverLoopback() throws Exception {
        ServerSocket listening = new ServerSocket(0); // ephemeral free port
        int port = listening.getLocalPort();
        listening.close();
        assertTrue("port must be > 0", port > 0);

        CompanionEndpoint phone = new CompanionEndpoint("phone-1", "AndroidPhone");
        CompanionEndpoint wear = new CompanionEndpoint("wear-1", "AndroidWear");

        SocketConnectionThread server = SocketConnectionThread.serverListen(port, phone);
        SocketConnectionThread client = SocketConnectionThread.clientConnect(port, wear);
        server.start();
        client.start();
        try {
            // Both ends exchange Connect handshake messages over the wire.
            assertTrue("phone did not connect", phone.connected.await(5, TimeUnit.SECONDS));
            assertTrue("wear did not connect", wear.connected.await(5, TimeUnit.SECONDS));
            assertTrue("phone did not receive wear Connect", phone.gotPeerConnect.await(5, TimeUnit.SECONDS));
            assertTrue("wear did not receive phone Connect", wear.gotPeerConnect.await(5, TimeUnit.SECONDS));
            assertNotNull(phone.getRemoteConnect());
            assertNotNull(wear.getRemoteConnect());
            assertEquals("wear-1", phone.getRemoteConnect().id);
            assertEquals("phone-1", wear.getRemoteConnect().id);

            // Phone pushes a notification into the established link.
            byte[] rawFrame = ancsFrame(0xBEEF, "org.microg.telegram", "New message");
            AncsNotifications.Notification notification = AncsNotifications.parse(rawFrame);
            long seq = WearableCompanionBridge.pushNotification(
                    phone.connection, "phone-1", 3L, "com.example.app", notification, rawFrame);
            assertEquals("pushNotification must write", 3L, seq);

            // Wear side receives the SetDataItem carrying the ANCS envelope.
            assertTrue("wear did not receive data item", wear.gotDataItem.await(5, TimeUnit.SECONDS));
            SetDataItem item = wear.dataItem;
            assertNotNull(item);
            assertEquals("phone-1", item.source);
            assertEquals("com.example.app", item.packageName);
            assertEquals(Long.valueOf(3L), item.seqId);
            assertEquals(Boolean.FALSE, item.deleted);
            assertTrue(item.uri, item.uri.startsWith(
                    "wear://phone-1" + WearableCompanionBridge.ANCS_NOTIFICATION_V1_PATH));

            AncsNotifications.Notification echoed =
                    WearableCompanionBridge.fromEnvelope(item.data.toByteArray());
            Assert.assertNotNull(echoed);
            assertEquals(0xBEEFL, echoed.getNotificationUid());
            assertEquals("org.microg.telegram", echoed.getAppIdentifier());
            assertEquals("New message", echoed.getTitle());
        } finally {
            client.close();
            server.interrupt(); // unblock serverListen's accept/reject loop cleanly (its close() races the null socket field)
        }
    }
}