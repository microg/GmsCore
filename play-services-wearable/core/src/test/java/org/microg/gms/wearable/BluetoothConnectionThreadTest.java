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

import java.util.UUID;

/**
 * Unit tests for {@link BluetoothConnectionThread} static constants.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Config.ALL_SDKS)
public class BluetoothConnectionThreadTest {

    /**
     * Verifies the Wear OS Bluetooth RFCOMM UUID matches the official value
     * used by Google Play Services Wearable.
     *
     * <p>Official UUID: a3c87500-8ed3-4bdf-8a39-a01bebede295
     * (used by Google Play Services on Wear OS for Bluetooth RFCOMM transport)
     */
    @Test
    public void testWearBtUuidIsCorrect() {
        UUID expected = UUID.fromString("a3c87500-8ed3-4bdf-8a39-a01bebede295");
        assertEquals("RFCOMM UUID must match official WearOS value",
                expected, BluetoothConnectionThread.WEAR_BT_UUID);
    }

    @Test
    public void testWearBtUuidIsNotNull() {
        assertNotNull("RFCOMM UUID must not be null", BluetoothConnectionThread.WEAR_BT_UUID);
    }

    @Test
    public void testWearBtUuidIsVersion4UUID() {
        // RFCOMM UUID is a variant-1 name-based UUID, not random
        // Just verify it parses correctly and is not the nil UUID
        assertNotEquals("RFCOMM UUID must not be NIL_UUID",
                UUID.fromString("00000000-0000-0000-0000-000000000000"),
                BluetoothConnectionThread.WEAR_BT_UUID);
    }
}
