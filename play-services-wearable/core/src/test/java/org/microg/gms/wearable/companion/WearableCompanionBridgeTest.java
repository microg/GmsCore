/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable.companion;

import junit.framework.TestCase;

import org.junit.Assert;
import org.microg.gms.wearable.notification.AncsNotifications;
import org.microg.wearable.proto.RootMessage;

import java.io.ByteArrayOutputStream;

public class WearableCompanionBridgeTest extends TestCase {

    private static void append(int attributeId, byte[] value, ByteArrayOutputStream out) {
        out.write(attributeId);
        out.write(value.length & 0xFF);
        out.write((value.length >>> 8) & 0xFF);
        out.write(value, 0, value.length);
    }

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

    public void testEnvelopeAndRoundTrip() throws Exception {
        byte[] rawFrame = ancsFrame(42L, "org.microg.telegram", "New message");
        AncsNotifications.Notification notification = AncsNotifications.parse(rawFrame);

        byte[] envelope = WearableCompanionBridge.envelope(notification, rawFrame);
        Assert.assertNotNull(envelope);
        assertEquals(WearableCompanionBridge.ANCS_ENVELOPE_VERSION, envelope[0] & 0xFF);
        assertEquals(42L,
                (envelope[1] & 0xFFL) | ((envelope[2] & 0xFFL) << 8)
                        | ((envelope[3] & 0xFFL) << 16) | ((envelope[4] & 0xFFL) << 24));

        // Round-trip through the envelope back to a parsed notification.
        AncsNotifications.Notification roundTripped =
                WearableCompanionBridge.fromEnvelope(envelope);
        Assert.assertNotNull(roundTripped);
        assertEquals(42L, roundTripped.getNotificationUid());
        assertEquals("org.microg.telegram", roundTripped.getAppIdentifier());
        assertEquals("New message", roundTripped.getTitle());
    }

    public void testRejectsUnknownEnvelopeVersion() throws Exception {
        byte[] rawFrame = ancsFrame(1L, "a", "b");
        byte[] envelope = WearableCompanionBridge.envelope(
                AncsNotifications.parse(rawFrame), rawFrame);
        envelope[0] = (byte) 0x63;
        Assert.assertNull(WearableCompanionBridge.fromEnvelope(envelope));
    }

    public void testRejectsOversizedPayload() throws Exception {
        byte[] rawFrame = ancsFrame(1L, "a", new String(new char[20 * 1024]).replace('\0', 'x'));
        byte[] oversized = WearableCompanionBridge.envelope(
                AncsNotifications.parse(rawFrame), rawFrame);
        Assert.assertNull(oversized); // exceeds MAX_ANCS_PAYLOAD
    }

    public void testEnvelopeRoundTripBackToSetDataItem() throws Exception {
        byte[] rawFrame = ancsFrame(7L, "org.microg.sms", "SMS");
        AncsNotifications.Notification notification = AncsNotifications.parse(rawFrame);

        // Phone serializes the SetDataItem exactly as it would be written on the wire.
        org.microg.wearable.proto.SetDataItem item = WearableCompanionBridge.toSetDataItem(
                "phone-node", 3L, "com.example.app", notification, rawFrame);
        Assert.assertNotNull(item);
        String uri = item.uri;
        Assert.assertTrue(uri, uri.startsWith(
                "wear://phone-node" + WearableCompanionBridge.ANCS_NOTIFICATION_V1_PATH));
        assertEquals("com.example.app", item.packageName);
        assertEquals(3L, (long) item.seqId);
        assertEquals(Boolean.FALSE, item.deleted);
        assertEquals("phone-node", item.source);

        // Wear side decodes the SetDataItem and recovers the same notification from the envelope.
        byte[] payload = item.data.toByteArray();
        AncsNotifications.Notification echoed = WearableCompanionBridge.fromEnvelope(payload);
        Assert.assertNotNull(echoed);
        assertEquals(7L, echoed.getNotificationUid());
        assertEquals("org.microg.sms", echoed.getAppIdentifier());
        assertEquals("SMS", echoed.getTitle());
    }
}