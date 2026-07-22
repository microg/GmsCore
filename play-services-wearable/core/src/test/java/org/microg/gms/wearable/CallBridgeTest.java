/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

/**
 * Unit tests for {@link CallBridge} encoding helpers and command constants.
 *
 * <p>These tests verify the binary protocol format that is exchanged with Wear OS peers,
 * ensuring compatibility with the official GmsCore implementation.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Config.ALL_SDKS)
public class CallBridgeTest {

    // -------------------------------------------------------------------------
    // encodeState() tests
    // -------------------------------------------------------------------------

    @Test
    public void testEncodeStateIdle() throws Exception {
        byte[] payload = CallBridge.encodeState((byte) 0, "", "");
        assertNotNull(payload);
        assertTrue(payload.length >= 3);

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(payload));
        assertEquals(0, dis.readByte());
        assertEquals("", dis.readUTF());
        assertEquals("", dis.readUTF());
    }

    @Test
    public void testEncodeStateRinging() throws Exception {
        byte[] payload = CallBridge.encodeState((byte) 1, "+1234567890", "John Doe");
        assertNotNull(payload);

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(payload));
        assertEquals(1, dis.readByte());
        assertEquals("+1234567890", dis.readUTF());
        assertEquals("John Doe", dis.readUTF());
    }

    @Test
    public void testEncodeStateOffhook() throws Exception {
        byte[] payload = CallBridge.encodeState((byte) 2, "+9876543210", "Jane Smith");
        assertNotNull(payload);

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(payload));
        assertEquals(2, dis.readByte());
        assertEquals("+9876543210", dis.readUTF());
        assertEquals("Jane Smith", dis.readUTF());
    }

    @Test
    public void testEncodeStateNullPhoneNumber() throws Exception {
        byte[] payload = CallBridge.encodeState((byte) 1, null, null);
        assertNotNull(payload);

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(payload));
        assertEquals(1, dis.readByte());
        assertEquals("", dis.readUTF());  // null treated as empty
        assertEquals("", dis.readUTF());
    }

    @Test
    public void testEncodeStateUnicodeContactName() throws Exception {
        byte[] payload = CallBridge.encodeState((byte) 1, "", "\u4e2d\u6587\u59d3\u540d");  // Chinese name
        assertNotNull(payload);

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(payload));
        assertEquals(1, dis.readByte());
        assertEquals("", dis.readUTF());
        assertEquals("\u4e2d\u6587\u59d3\u540d", dis.readUTF());
    }

    // -------------------------------------------------------------------------
    // handleCommand() null/empty payload tests
    // -------------------------------------------------------------------------

    @Test
    public void testHandleCommandNullPayload() {
        // Should not throw - gracefully handles null input
        CallBridge.handleCommand(null, null);
    }

    @Test
    public void testHandleCommandEmptyPayload() {
        // Should not throw - gracefully handles empty input
        CallBridge.handleCommand(null, new byte[0]);
    }

    @Test
    public void testHandleCommandGarbagePayload() {
        // Should not throw - gracefully handles malformed input
        CallBridge.handleCommand(null, new byte[] { (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF });
    }
}
