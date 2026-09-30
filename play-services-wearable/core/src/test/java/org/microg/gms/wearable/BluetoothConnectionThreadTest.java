/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.microg.wearable.SocketWearableConnection;
import org.microg.wearable.WearableConnection;
import org.microg.wearable.proto.RootMessage;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.UUID;

/**
 * Unit tests for {@link BluetoothConnectionThread} RFCOMM UUID policy.
 *
 * <p>The UUID is an intentional microG experimental constant. Tests lock the
 * chosen policy (stable value + non-null + not NIL) and explicitly reject the
 * false claim that this is a stock Wear OS companion RFCOMM UUID.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class BluetoothConnectionThreadTest {

    /** Stable microG experimental RFCOMM UUID (lab / microG-to-microG). */
    private static final UUID MICROG_EXPERIMENTAL_RFCOMM_UUID =
            UUID.fromString("a3c87500-8ed3-4bdf-8a39-a01bebede295");

    @Test
    public void testWearBtUuidMatchesMicrogExperimentalPolicy() {
        assertEquals(
                "RFCOMM UUID must remain the stable microG experimental value",
                MICROG_EXPERIMENTAL_RFCOMM_UUID,
                BluetoothConnectionThread.WEAR_BT_UUID);
    }

    @Test
    public void testWearBtUuidIsNotNull() {
        assertNotNull("RFCOMM UUID must not be null", BluetoothConnectionThread.WEAR_BT_UUID);
    }

    @Test
    public void testWearBtUuidIsNotNilUuid() {
        assertNotEquals(
                "RFCOMM UUID must not be NIL_UUID",
                UUID.fromString("00000000-0000-0000-0000-000000000000"),
                BluetoothConnectionThread.WEAR_BT_UUID);
    }

    @Test
    public void testWearBtServiceNameIsMicrogLabeled() {
        // Package-private constant is visible to tests in the same package.
        assertEquals("microG Wearable", BluetoothConnectionThread.WEAR_BT_SERVICE_NAME);
        assertFalse(
                "Service name must not imply stock Wear OS SDP identity alone",
                "WearOS".equals(BluetoothConnectionThread.WEAR_BT_SERVICE_NAME));
    }

    /**
     * T1 / P1-A: accepting peer B must not close peer A's socket.
     *
     * <p>Today {@code setWearableConnection} is a single volatile slot that
     * {@code prev.close()}s the previous connection. Server-mode workers all
     * call it on the shared thread, so the second RFCOMM peer kills the first.
     *
     * <p>This test is runtime-RED on purpose: it does not call a collection
     * API that does not exist yet (that would compile-fail the whole source
     * set and hide T2). After Slice 1, A must stay open.
     *
     * <p>Requires Wire 1.6.1 on the unit-test classpath (see core/build.gradle).
     * Wearable 0.1.1's {@code WearableConnection.&lt;clinit&gt;} is Wire 1.x;
     * Wire 6.x on the test classpath throws {@code NoSuchMethodError} before
     * these assertions run.
     */
    @Test
    public void testSecondSetWearableConnectionDoesNotCloseFirstPeer() throws Exception {
        BluetoothConnectionThread thread = new BluetoothConnectionThread() {
            @Override
            public void close() {
                // Test stub: no Bluetooth server/client socket to tear down.
            }
        };

        WearableConnection.Listener noop = new NoopWearableListener();
        RecordingSocket socketA = new RecordingSocket();
        RecordingSocket socketB = new RecordingSocket();
        SocketWearableConnection connA = new SocketWearableConnection(socketA, noop);
        SocketWearableConnection connB = new SocketWearableConnection(socketB, noop);

        thread.setWearableConnection(connA);
        assertSame(connA, thread.getWearableConnection());
        assertFalse("first peer must be open after it is installed", socketA.isClosed());

        thread.setWearableConnection(connB);

        assertFalse(
                "first peer socket must stay open when a second peer is accepted",
                socketA.isClosed());
        assertFalse("second peer socket must stay open", socketB.isClosed());
        assertSame(
                "getWearableConnection still reports the most recent peer",
                connB,
                thread.getWearableConnection());
    }

    private static final class NoopWearableListener implements WearableConnection.Listener {
        @Override
        public void onConnected(WearableConnection connection) {
        }

        @Override
        public void onMessage(WearableConnection connection, RootMessage message) {
        }

        @Override
        public void onDisconnected() {
        }
    }

    /**
     * Unconnected {@code new Socket()} throws from {@code getInputStream()}.
     * This stub supplies streams and records {@code close()} so T1 can observe
     * whether {@code setWearableConnection} tears down the previous peer.
     */
    private static final class RecordingSocket extends Socket {
        private final ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private volatile boolean closed;

        @Override
        public InputStream getInputStream() {
            return in;
        }

        @Override
        public OutputStream getOutputStream() {
            return out;
        }

        @Override
        public synchronized void close() throws IOException {
            closed = true;
        }

        @Override
        public boolean isClosed() {
            return closed;
        }
    }
}
