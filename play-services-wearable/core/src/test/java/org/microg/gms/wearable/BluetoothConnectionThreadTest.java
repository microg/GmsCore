/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import java.util.UUID;

/**
 * Verifies Wear OS Bluetooth RFCOMM UUIDs against documented research values
 * (teccheck/wearos-research). Several bounty PRs used incorrect UUIDs.
 */
public class BluetoothConnectionThreadTest {

    @Test
    public void wearableBtUuidMatchesResearch() {
        assertEquals(
                UUID.fromString("5e8945b0-9525-11e3-a5e2-0800200c9a66"),
                BluetoothConnectionThread.WEARABLE_BT_UUID);
    }

    @Test
    public void flowUuidMatchesResearch() {
        assertEquals(
                UUID.fromString("fafbdd20-83f0-4389-addf-917ac9dae5b2"),
                BluetoothConnectionThread.FLOW_UUID);
    }

    @Test
    public void flow15UuidMatchesResearch() {
        assertEquals(
                UUID.fromString("6a1eafb1-61c0-42a0-8bb0-a336fb1c3f00"),
                BluetoothConnectionThread.FLOW15_UUID);
    }

    @Test
    public void uuidsAreDistinctAndNonNil() {
        UUID nil = UUID.fromString("00000000-0000-0000-0000-000000000000");
        assertNotNull(BluetoothConnectionThread.WEARABLE_BT_UUID);
        assertNotEquals(nil, BluetoothConnectionThread.WEARABLE_BT_UUID);
        assertNotEquals(BluetoothConnectionThread.WEARABLE_BT_UUID, BluetoothConnectionThread.FLOW_UUID);
        assertNotEquals(BluetoothConnectionThread.FLOW_UUID, BluetoothConnectionThread.FLOW15_UUID);
        // Common incorrect values seen in prior PRs
        assertNotEquals(
                UUID.fromString("00000003-0000-1000-8000-00805f9b34fb"),
                BluetoothConnectionThread.WEARABLE_BT_UUID);
        assertNotEquals(
                UUID.fromString("a3c87500-8ed3-4bdf-8a39-a01bebede295"),
                BluetoothConnectionThread.WEARABLE_BT_UUID);
    }
}
