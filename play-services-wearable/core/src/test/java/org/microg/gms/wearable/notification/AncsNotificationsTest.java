/*
 * Copyright (C) 2026 microG Project Team
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

package org.microg.gms.wearable.notification;

import junit.framework.TestCase;

import org.junit.Assert;

import java.io.ByteArrayOutputStream;

public class AncsNotificationsTest extends TestCase {

    private static void append(int attributeId, byte[] value, ByteArrayOutputStream out) {
        out.write(attributeId);
        out.write(value.length & 0xFF);
        out.write((value.length >>> 8) & 0xFF);
        out.write(value, 0, value.length);
    }

    private static byte[] frame(int commandId, long uid, int attributeId, String value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write((byte) commandId);
        out.write((int) (uid & 0xFF));
        out.write((int) ((uid >>> 8) & 0xFF));
        out.write((int) ((uid >>> 16) & 0xFF));
        out.write((int) ((uid >>> 24) & 0xFF));
        append(attributeId, value.getBytes(java.nio.charset.StandardCharsets.UTF_8), out);
        return out.toByteArray();
    }

    public void testParseCompleteFrame() throws Exception {
        byte[] value = "org.microg.telegram\0".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(AncsNotifications.COMMAND_GET_NOTIFICATION_ATTRIBUTES);
        out.write(new byte[]{0x2A, 0x00, 0x00, 0x00}); // uid 42
        append(AncsNotifications.ATTR_APP_IDENTIFIER, value, out);
        append(AncsNotifications.ATTR_TITLE, "New message".getBytes(java.nio.charset.StandardCharsets.UTF_8), out);
        append(AncsNotifications.ATTR_MESSAGE, "ping".getBytes(java.nio.charset.StandardCharsets.UTF_8), out);

        AncsNotifications.Notification notification = AncsNotifications.parse(out.toByteArray());

        assertEquals(42L, notification.getNotificationUid());
        assertEquals("org.microg.telegram", notification.getAppIdentifier());
        assertEquals("New message", notification.getTitle());
        assertEquals("ping", notification.getMessage());
        Assert.assertNull(notification.getSubtitle());
    }

    public void testParseMessageSizeIsUInt32() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(AncsNotifications.COMMAND_GET_NOTIFICATION_ATTRIBUTES);
        out.write(new byte[]{0x01, 0x00, 0x00, 0x00}); // uid 1
        append(AncsNotifications.ATTR_TITLE, "t".getBytes(java.nio.charset.StandardCharsets.UTF_8), out);
        out.write(AncsNotifications.ATTR_MESSAGE_SIZE);
        out.write(new byte[]{4, 0});
        out.write(new byte[]{(byte) 0xE8, 0x03, 0x00, 0x00}); // 1000

        AncsNotifications.Notification notification = AncsNotifications.parse(out.toByteArray());

        assertEquals(1000L, notification.getMessageSize());
    }

    public void testUnknownAttributeIsSkipped() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(AncsNotifications.COMMAND_GET_NOTIFICATION_ATTRIBUTES);
        out.write(new byte[]{0x05, 0x00, 0x00, 0x00});
        append(0x42, "future".getBytes(java.nio.charset.StandardCharsets.UTF_8), out);
        append(AncsNotifications.ATTR_TITLE, "ok".getBytes(java.nio.charset.StandardCharsets.UTF_8), out);

        AncsNotifications.Notification notification = AncsNotifications.parse(out.toByteArray());

        assertEquals("ok", notification.getTitle());
    }

    public void testRejectsTruncatedFrame() throws Exception {
        byte[] frame = frame(AncsNotifications.COMMAND_GET_NOTIFICATION_ATTRIBUTES, 7, AncsNotifications.ATTR_TITLE, "x");
        try {
            AncsNotifications.parse(new byte[]{frame[0], frame[1], frame[2]});
            Assert.fail("expected IllegalArgumentException for truncated frame");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    public void testRejectsEmptyFrame() throws Exception {
        try {
            AncsNotifications.parse(new byte[0]);
            Assert.fail("expected IllegalArgumentException for empty frame");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    public void testRejectsWrongCommandId() throws Exception {
        byte[] frame = frame(0x01, 7, AncsNotifications.ATTR_TITLE, "x");
        try {
            AncsNotifications.parse(frame);
            Assert.fail("expected IllegalArgumentException for unexpected command id");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    public void testBuildsReadAttributesRequest() throws Exception {
        byte[] request = AncsNotifications.readAttributesRequest(0x01020304,
                AncsNotifications.ATTR_APP_IDENTIFIER, AncsNotifications.ATTR_TITLE, AncsNotifications.ATTR_MESSAGE);

        assertEquals(8, request.length);
        assertEquals(AncsNotifications.COMMAND_GET_NOTIFICATION_ATTRIBUTES, request[0] & 0xFF);
        assertEquals(0x04, request[1] & 0xFF);
        assertEquals(0x03, request[2] & 0xFF);
        assertEquals(0x02, request[3] & 0xFF);
        assertEquals(0x01, request[4] & 0xFF);
        assertEquals(AncsNotifications.ATTR_APP_IDENTIFIER, request[5] & 0xFF);
        assertEquals(AncsNotifications.ATTR_TITLE, request[6] & 0xFF);
        assertEquals(AncsNotifications.ATTR_MESSAGE, request[7] & 0xFF);
    }
}