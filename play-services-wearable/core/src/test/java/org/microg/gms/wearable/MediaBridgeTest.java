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

/**
 * Unit tests for {@link MediaBridge} command parsing robustness.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class MediaBridgeTest {

    @Test
    public void testHandleCommandNullPayload() {
        MediaBridge.handleCommand(null, null);
    }

    @Test
    public void testHandleCommandEmptyPayload() {
        MediaBridge.handleCommand(null, new byte[0]);
    }

    @Test
    public void testHandleCommandGarbagePayload() {
        MediaBridge.handleCommand(null, new byte[] { (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF });
    }

    @Test
    public void testMediaPaths() {
        assertEquals("/wearable/media", MediaBridge.MEDIA_PATH);
        assertEquals("/wearable/media/command", MediaBridge.MEDIA_COMMAND_PATH);
    }
}
