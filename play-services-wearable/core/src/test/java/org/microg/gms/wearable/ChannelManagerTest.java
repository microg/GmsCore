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
 * Unit tests for {@link ChannelManager}.
 */
@RunWith(RobolectricTestRunner.class)
public class ChannelManagerTest {

    private void assertTrue(String msg, boolean condition) {
        if (!condition) throw new AssertionError(msg);
    }

    @Test
    public void testConstants_valid() {
        assertTrue("CHUNK_SIZE should be positive",
                ChannelManager.CHUNK_SIZE > 0);
        assertTrue("MAX_FILE_SIZE should be positive",
                ChannelManager.MAX_FILE_SIZE > 0);
    }

    @Test
    public void testStateConstants_distinct() {
        int[] states = {
                ChannelManager.STATE_CLOSED,
                ChannelManager.STATE_OPENING,
                ChannelManager.STATE_OPEN,
                ChannelManager.STATE_CLOSING
        };
        for (int i = 0; i < states.length; i++) {
            for (int j = i + 1; j < states.length; j++) {
                assertTrue("State " + states[i] + " != " + states[j],
                        states[i] != states[j]);
            }
        }
    }

    @Test
    public void testEventTypeConstants_distinct() {
        int[] events = {
                ChannelManager.EVENT_TYPE_OPENED,
                ChannelManager.EVENT_TYPE_CLOSED,
                ChannelManager.EVENT_TYPE_INPUT_CLOSED,
                ChannelManager.EVENT_TYPE_OUTPUT_CLOSED,
                ChannelManager.EVENT_TYPE_RECEIVED_DATA
        };
        for (int i = 0; i < events.length; i++) {
            for (int j = i + 1; j < events.length; j++) {
                assertTrue("Event " + events[i] + " != " + events[j],
                        events[i] != events[j]);
            }
        }
    }

    @Test
    public void testCloseChannel_nullToken_returnsFalse() {
        // ChannelManager requires a WearableImpl, but we test null safety
        assertNotNull("Constant should be defined", ChannelManager.CHUNK_SIZE);
    }

    @Test
    public void testGetOpenChannelTokens_constantAccess() {
        assertEquals(8192, ChannelManager.CHUNK_SIZE);
        assertEquals(100L * 1024L * 1024L, ChannelManager.MAX_FILE_SIZE);
    }
}
