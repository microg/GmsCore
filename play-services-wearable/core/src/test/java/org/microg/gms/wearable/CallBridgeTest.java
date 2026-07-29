/*
 * SPDX-FileCopyrightText: 2024-2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Unit tests for {@link CallBridge}.
 */
@RunWith(RobolectricTestRunner.class)
public class CallBridgeTest {

    @Test
    public void testStateConstants_distinct() {
        // Verify state constants are distinct
        byte[] states = {
                CallBridge.STATE_IDLE,
                CallBridge.STATE_RINGING,
                CallBridge.STATE_ANSWERED,
                CallBridge.STATE_DISCONNECTED
        };
        for (int i = 0; i < states.length; i++) {
            for (int j = i + 1; j < states.length; j++) {
                assertTrue("State " + states[i] + " should differ from " + states[j],
                        states[i] != states[j]);
            }
        }
    }

    private void assertTrue(String msg, boolean condition) {
        if (!condition) throw new AssertionError(msg);
    }

    @Test
    public void testCommandConstants_distinct() {
        byte[] commands = {
                CallBridge.CMD_ACCEPT,
                CallBridge.CMD_REJECT,
                CallBridge.CMD_MUTE_TOGGLE,
                CallBridge.CMD_SILENCE_RINGER
        };
        for (int i = 0; i < commands.length; i++) {
            for (int j = i + 1; j < commands.length; j++) {
                assertTrue("Command " + commands[i] + " should differ from " + commands[j],
                        commands[i] != commands[j]);
            }
        }
    }

    @Test
    public void testInitialState_isIdle() {
        assertEquals("Initial state should be IDLE",
                CallBridge.STATE_IDLE, CallBridge.getCurrentState());
    }

    @Test
    public void testLastIncomingNumber_initialEmpty() {
        assertEquals("Initial last number should be empty",
                "", CallBridge.getLastIncomingNumber());
    }

    @Test
    public void testCallPath_constant() {
        assertEquals("/wearable/call", CallBridge.CALL_PATH);
        assertEquals("/wearable/call/command", CallBridge.CALL_COMMAND_PATH);
    }

    @Test
    public void testSerializeCallState_idle() {
        byte[] result = CallBridge.serializeCallState(
                CallBridge.STATE_IDLE, "", "");
        assertNotNull(result);
        assertEquals("First byte should be STATE_IDLE",
                CallBridge.STATE_IDLE, result[0]);
    }

    @Test
    public void testSerializeCallState_ringing() {
        byte[] result = CallBridge.serializeCallState(
                CallBridge.STATE_RINGING, "1234567890", "John Doe");
        assertNotNull(result);
        assertEquals("First byte should be STATE_RINGING",
                CallBridge.STATE_RINGING, result[0]);
    }

    @Test
    public void testHandleCommand_unknownCommand() {
        boolean result = CallBridge.handleCommand(null, (byte) 99);
        assertEquals("Unknown command should return false", false, result);
    }
}
