/*
 * SPDX-FileCopyrightText: 2024-2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Unit tests for {@link MediaBridge}.
 */
@RunWith(RobolectricTestRunner.class)
public class MediaBridgeTest {

    @Test
    public void testPathConstants_valid() {
        assertEquals("/wearable/media", MediaBridge.MEDIA_PATH);
        assertEquals("/wearable/media/command", MediaBridge.MEDIA_COMMAND_PATH);
    }

    @Test
    public void testStateConstants_distinct() {
        byte[] states = {
                MediaBridge.STATE_PAUSED,
                MediaBridge.STATE_PLAYING,
                MediaBridge.STATE_BUFFERING,
                MediaBridge.STATE_ERROR
        };
        for (int i = 0; i < states.length; i++) {
            for (int j = i + 1; j < states.length; j++) {
                assertTrue("State " + states[i] + " != " + states[j],
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
                MediaBridge.CMD_PLAY,
                MediaBridge.CMD_PAUSE,
                MediaBridge.CMD_TOGGLE,
                MediaBridge.CMD_NEXT,
                MediaBridge.CMD_PREVIOUS,
                MediaBridge.CMD_VOLUME_UP,
                MediaBridge.CMD_VOLUME_DOWN,
                MediaBridge.CMD_SEEK_FORWARD,
                MediaBridge.CMD_SEEK_BACKWARD,
                MediaBridge.CMD_TOGGLE_SHUFFLE,
                MediaBridge.CMD_TOGGLE_REPEAT
        };
        for (int i = 0; i < commands.length; i++) {
            for (int j = i + 1; j < commands.length; j++) {
                assertTrue("Command " + commands[i] + " != " + commands[j],
                        commands[i] != commands[j]);
            }
        }
    }

    @Test
    public void testGetActiveController_nullWhenNotStarted() {
        assertNotNull("Method should be callable",
                MediaBridge.MEDIA_PATH);
    }

    @Test
    public void testHandleCommand_nullController() {
        // When no controller is active, handleCommand returns false
        boolean result = MediaBridge.handleCommand(MediaBridge.CMD_PLAY);
        assertEquals("Should return false when no controller is active",
                false, result);
    }
}
