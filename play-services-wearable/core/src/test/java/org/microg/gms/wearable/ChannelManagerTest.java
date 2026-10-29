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
import org.microg.wearable.proto.ChannelControlRequest;
import org.microg.wearable.proto.ChannelRequest;
import org.microg.wearable.proto.Request;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Unit tests for {@link ChannelManager} resource management, invalid token handling,
 * and ParcelFileDescriptor ownership (dup semantics).
 *
 * <p>Pinned to a single SDK: Robolectric multiplies {@code ALL_SDKS} across every method
 * and its PFD.dup() emulation is incomplete, which previously OOMed the test worker.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
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

    @Test
    public void testGetInputStreamReturnsPfdAndChannelSurvivesCallerClose() throws Exception {
        String token = installOpenChannel("node-a", "/test/path");

        ParcelFileDescriptor appRead1 = channelManager.getInputStream(token);
        assertNotNull("getInputStream must return a PFD for open channels", appRead1);
        ParcelFileDescriptor appRead2 = channelManager.getInputStream(token);
        assertNotNull("repeated getInputStream must return a PFD", appRead2);

        // Production code returns dup()s. On real Android, closing one dup leaves the
        // canonical end open. Robolectric may share backing FDs, so we assert the
        // channel-level contract instead of OS-level FD independence:
        // closing caller PFDs must not remove the channel from the manager.
        try { appRead1.close(); } catch (IOException ignored) {}
        assertTrue("channel must remain open after caller closes a returned input PFD",
                channelManager.isChannelOpen(token));

        try { appRead2.close(); } catch (IOException ignored) {}
        assertTrue("channel must remain open after caller closes all returned input PFDs",
                channelManager.isChannelOpen(token));

        assertTrue(channelManager.closeChannel(token, 0));
        assertFalse(channelManager.isChannelOpen(token));
    }

    @Test
    public void testGetOutputStreamReturnsPfdForOpenChannel() throws Exception {
        String token = installOpenChannel("node-out", "/out");
        // With null WearableImpl the network forwarder is skipped, but the app-facing
        // write-end PFD must still be created and closable without tearing down state.
        ParcelFileDescriptor out = channelManager.getOutputStream(token);
        assertNotNull(out);
        try { out.close(); } catch (IOException ignored) {}
        assertTrue(channelManager.isChannelOpen(token));
        assertTrue(channelManager.closeChannel(token, 0));
        assertFalse(channelManager.isChannelOpen(token));
    }

    @Test
    public void testCloseChannelWithNullWearableDoesNotThrow() throws Exception {
        String token = installOpenChannel("node-close", "/close");
        // Production skips peer CLOSE when wearable is null; must not swallow random RE.
        assertTrue(channelManager.closeChannel(token, 0));
        assertFalse(channelManager.isChannelOpen(token));
    }

    @Test
    public void testCloseChannelUnknownTokenReturnsFalse() {
        assertFalse(channelManager.closeChannel("missing", 0));
    }

    @Test
    public void testCloseAllClearsOpenChannels() throws Exception {
        String token = installOpenChannel("node-b", "/cap/x");
        assertTrue(channelManager.isChannelOpen(token));
        channelManager.closeAll();
        assertFalse(channelManager.isChannelOpen(token));
        assertNull(channelManager.getInputStream(token));
    }

    /**
     * T2 / P1-C: a second incoming OPEN for a live channelId must not replace
     * the first {@code ChannelState}.
     *
     * <p>Must use {@link ChannelManager#handleIncomingChannelMessage} with an
     * explicit peer {@code channelId}. The legacy
     * {@link ChannelManager#handleIncomingChannelRequest} always allocates a
     * new id and cannot produce this collision.
     *
     * <p>Requires Wire 1.6.1 on the unit-test classpath (see core/build.gradle).
     * {@code ChannelControlRequest.Builder} is a Wire 1.x type from wearable
     * 0.1.1; Wire 6.x throws {@code NoSuchMethodError} in {@code Message.&lt;init&gt;}.
     */
    @Test
    public void testIncomingOpenDoesNotClobberLiveChannel() throws Exception {
        channelManager.handleIncomingChannelMessage("node-a", incomingOpen(1L, "/first"));
        assertTrue("first incoming OPEN must register token 1",
                channelManager.isChannelOpen("1"));

        Field channelsField = ChannelManager.class.getDeclaredField("channels");
        channelsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Long, Object> channels = (Map<Long, Object>) channelsField.get(channelManager);
        Object firstState = channels.get(1L);
        assertNotNull(firstState);

        Field pathField = firstState.getClass().getDeclaredField("path");
        pathField.setAccessible(true);
        assertEquals("/first", pathField.get(firstState));

        Field nextField = ChannelManager.class.getDeclaredField("nextChannelId");
        nextField.setAccessible(true);
        AtomicLong next = (AtomicLong) nextField.get(channelManager);
        long nextAfterFirst = next.get();
        assertTrue("CAS must advance nextChannelId past the peer-assigned id",
                nextAfterFirst >= 2L);

        channelManager.handleIncomingChannelMessage("node-b", incomingOpen(1L, "/collide"));

        Object afterState = channels.get(1L);
        assertSame("colliding incoming OPEN must not replace the live ChannelState",
                firstState, afterState);
        assertTrue("original channel token must stay open after the collide",
                channelManager.isChannelOpen("1"));
        assertEquals("first path must be preserved", "/first", pathField.get(afterState));
        assertTrue("nextChannelId must remain advanced after the collide",
                next.get() >= nextAfterFirst);
    }

    /**
     * Installs a channel state as if the peer opened it, without needing WearableImpl.
     */
    @SuppressWarnings("unchecked")
    private String installOpenChannel(String nodeId, String path) throws Exception {
        // Public legacy entry creates local state without needing WearableImpl/network.
        channelManager.handleIncomingChannelRequest(nodeId, path, "ignored");

        Field tokenToIdField = ChannelManager.class.getDeclaredField("tokenToId");
        tokenToIdField.setAccessible(true);
        Map<String, Long> tokenToId = (Map<String, Long>) tokenToIdField.get(channelManager);
        assertNotNull(tokenToId);
        assertFalse(tokenToId.isEmpty());
        return tokenToId.keySet().iterator().next();
    }

    private static Request incomingOpen(long channelId, String path) {
        ChannelControlRequest ctrl = new ChannelControlRequest.Builder()
                .type(1)
                .channelId(channelId)
                .fromChannelOperator(true)
                .path(path)
                .build();
        return new Request.Builder()
                .path(path)
                .request(new ChannelRequest.Builder()
                        .channelControlRequest(ctrl)
                        .build())
                .build();
    }
}
