/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable.bridge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

public class NotificationPayloadTest {

    @Test
    public void roundTripPreservesFields() {
        NotificationPayload original = new NotificationPayload(
                "pkg|0|42",
                "com.example.mail",
                42,
                "Subject\nline2",
                "Hello=body",
                "email",
                1,
                false,
                Arrays.asList("Reply", "Archive"));

        NotificationPayload decoded = NotificationPayload.fromBytes(original.toBytes());

        assertEquals(original.key, decoded.key);
        assertEquals(original.packageName, decoded.packageName);
        assertEquals(original.id, decoded.id);
        assertEquals(original.title, decoded.title);
        assertEquals(original.text, decoded.text);
        assertEquals(original.category, decoded.category);
        assertEquals(original.priority, decoded.priority);
        assertEquals(original.ongoing, decoded.ongoing);
        assertEquals(original.actions, decoded.actions);
    }

    @Test
    public void removedPayloadContainsIdentity() {
        byte[] bytes = NotificationPayload.removedBytes("key-1", "com.example", 7);
        Map<String, String> fields = NotificationPayload.decode(bytes);
        assertEquals("key-1", fields.get("key"));
        assertEquals("com.example", fields.get("packageName"));
        assertEquals("7", fields.get("id"));
    }

    @Test
    public void emptyActionsRoundTrip() {
        NotificationPayload payload = new NotificationPayload(
                "k", "p", 1, "t", "x", "c", 0, false, Collections.<String>emptyList());
        NotificationPayload decoded = NotificationPayload.fromBytes(payload.toBytes());
        assertEquals(0, decoded.actions.size());
        assertFalse(decoded.ongoing);
    }
}
