/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import static org.junit.Assert.*;

import android.os.ParcelFileDescriptor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;

/**
 * Unit tests for {@link ChannelManager} resource management and invalid token handling.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Config.ALL_SDKS)
public class ChannelManagerTest {

    private ChannelManager channelManager;

    @Before
    public void setUp() {
        channelManager = new ChannelManager(null);
    }

    @Test
    public void testWriteInputToFdInvalidTokenClosesFd() throws Exception {
        ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        ParcelFileDescriptor writeEnd = pipe[1];

        boolean result = channelManager.writeInputToFd("invalid-token", writeEnd);
        assertFalse("writeInputToFd should return false for invalid token", result);

        try {
            assertFalse("FD should be invalid or closed", writeEnd.getFileDescriptor().valid());
        } catch (IllegalStateException e) {
            // Expected when ParcelFileDescriptor is closed in Robolectric
        }
        try { pipe[0].close(); } catch (IOException ignored) {}
    }

    @Test
    public void testReadOutputFromFdInvalidTokenClosesFd() throws Exception {
        ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        ParcelFileDescriptor readEnd = pipe[0];

        boolean result = channelManager.readOutputFromFd("invalid-token", readEnd, 0, -1);
        assertFalse("readOutputFromFd should return false for invalid token", result);

        try {
            assertFalse("FD should be invalid or closed", readEnd.getFileDescriptor().valid());
        } catch (IllegalStateException e) {
            // Expected when ParcelFileDescriptor is closed in Robolectric
        }
        try { pipe[1].close(); } catch (IOException ignored) {}
    }

    @Test
    public void testGetInputStreamUnknownTokenReturnsNull() {
        assertNull(channelManager.getInputStream("non-existent"));
    }

    @Test
    public void testGetOutputStreamUnknownTokenReturnsNull() {
        assertNull(channelManager.getOutputStream("non-existent"));
    }
}
