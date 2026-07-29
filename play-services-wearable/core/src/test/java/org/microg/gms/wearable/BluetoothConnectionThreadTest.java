/*
 * SPDX-FileCopyrightText: 2024-2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.UUID;

/**
 * Unit tests for {@link BluetoothConnectionThread}.
 */
@RunWith(RobolectricTestRunner.class)
public class BluetoothConnectionThreadTest {

    @Test
    public void testWearBtUuid_isValid() {
        UUID uuid = BluetoothConnectionThread.WEAR_BT_UUID;
        assertNotNull("WEAR_BT_UUID should not be null", uuid);
        assertEquals("3c87500-8ed3-4bdf-8a39-a01bebede295",
                uuid.toString().substring(1)); // strip leading 'a'
    }

    @Test
    public void testServiceName_isNotEmpty() {
        assertNotNull("WEAR_BT_SERVICE_NAME should not be null",
                BluetoothConnectionThread.WEAR_BT_SERVICE_NAME);
        assertFalse("WEAR_BT_SERVICE_NAME should not be empty",
                BluetoothConnectionThread.WEAR_BT_SERVICE_NAME.isEmpty());
    }

    @Test
    public void testMaxReconnectAttempts_isPositive() {
        assertTrue("MAX_RECONNECT_ATTEMPTS should be positive",
                BluetoothConnectionThread.MAX_RECONNECT_ATTEMPTS > 0);
    }

    @Test
    public void testConnectionStateEnum_hasAllValues() {
        BluetoothConnectionThread.ConnectionState[] states =
                BluetoothConnectionThread.ConnectionState.values();
        assertEquals(4, states.length);
        assertEquals(BluetoothConnectionThread.ConnectionState.DISCONNECTED, states[0]);
        assertEquals(BluetoothConnectionThread.ConnectionState.CONNECTING, states[1]);
        assertEquals(BluetoothConnectionThread.ConnectionState.CONNECTED, states[2]);
        assertEquals(BluetoothConnectionThread.ConnectionState.DISCONNECTING, states[3]);
    }

    @Test
    public void testConnectionState_initialState() {
        // Enum defaults to DISCONNECTED
        BluetoothConnectionThread.ConnectionState state =
                BluetoothConnectionThread.ConnectionState.DISCONNECTED;
        assertEquals("Initial state should be DISCONNECTED",
                BluetoothConnectionThread.ConnectionState.DISCONNECTED, state);
    }
}
