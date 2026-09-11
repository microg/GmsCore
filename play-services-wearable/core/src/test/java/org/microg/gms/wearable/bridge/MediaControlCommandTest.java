/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable.bridge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MediaControlCommandTest {

    @Test
    public void parsesNamedActions() {
        assertEquals(MediaControlCommand.Action.PLAY,
                MediaControlCommand.parse("/wearable/media/control/play", null).action);
        assertEquals(MediaControlCommand.Action.PAUSE,
                MediaControlCommand.parse("/wearable/media/control/pause", null).action);
        assertEquals(MediaControlCommand.Action.TOGGLE,
                MediaControlCommand.parse("/wearable/media/control/toggle", null).action);
        assertEquals(MediaControlCommand.Action.NEXT,
                MediaControlCommand.parse("/wearable/media/control/next", null).action);
        assertEquals(MediaControlCommand.Action.PREVIOUS,
                MediaControlCommand.parse("/wearable/media/control/previous", null).action);
    }

    @Test
    public void parsesSeekAndRatePayloads() {
        byte[] seek = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(12345L).array();
        MediaControlCommand seekCmd = MediaControlCommand.parse("/wearable/media/control/seek", seek);
        assertNotNull(seekCmd);
        assertEquals(MediaControlCommand.Action.SEEK, seekCmd.action);
        assertEquals(12345L, seekCmd.seekPositionMs);

        byte[] rate = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putFloat(1.5f).array();
        MediaControlCommand rateCmd = MediaControlCommand.parse("/wearable/media/control/rate", rate);
        assertNotNull(rateCmd);
        assertEquals(MediaControlCommand.Action.RATE, rateCmd.action);
        assertEquals(1.5f, rateCmd.playbackRate, 0.001f);
    }

    @Test
    public void rejectsUnknownPaths() {
        assertFalse(MediaControlCommand.isControlPath("/other"));
        assertNull(MediaControlCommand.parse("/wearable/media/control/unknown", null));
        assertNull(MediaControlCommand.parse("/wearable/notification/posted", null));
    }

    @Test
    public void controlPathPrefixRecognized() {
        assertTrue(MediaControlCommand.isControlPath("/wearable/media/control/play"));
        assertEquals("/wearable/media/state", MediaControlCommand.STATE_PATH);
    }
}
