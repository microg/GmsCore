/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable.bluetooth;

import org.junit.Test;
import org.microg.gms.wearable.WearableNotificationPayload;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WearableBtUuidsTest {
    @Test
    public void wearableBtUuidIsNotGenericSpp() {
        UUID spp = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        assertFalse(spp.equals(WearableBtUuids.WEARABLE_BT));
        assertEquals("WearableBt", WearableBtUuids.NAME_WEARABLE_BT);
        assertEquals(3, WearableBtUuids.ALL.size());
        assertEquals(WearableBtUuids.WEARABLE_BT, WearableBtUuids.ALL.get(0).uuid);
    }

    @Test
    public void bluetoothAddressDetection() {
        assertTrue(WearableBtUuids.isBluetoothAddress("AA:BB:CC:DD:EE:FF"));
        assertTrue(WearableBtUuids.isBluetoothAddress("a1:b2:c3:d4:e5:f6"));
        assertFalse(WearableBtUuids.isBluetoothAddress("server"));
        assertFalse(WearableBtUuids.isBluetoothAddress("NULL_STRING"));
        assertFalse(WearableBtUuids.isBluetoothAddress(""));
        assertFalse(WearableBtUuids.isBluetoothAddress(null));
        assertFalse(WearableBtUuids.isBluetoothAddress("192.168.1.1"));
    }

    @Test
    public void notificationPayloadRoundTrip() {
        WearableNotificationPayload original = new WearableNotificationPayload(
                "0|com.example.app|1|tag|1000",
                "com.example.app",
                "Hello \"world\"",
                "Line1\nLine2",
                true,
                42
        );
        WearableNotificationPayload parsed = WearableNotificationPayload.parse(original.toJson());
        assertEquals(original.key, parsed.key);
        assertEquals(original.packageName, parsed.packageName);
        assertEquals(original.title, parsed.title);
        assertEquals(original.text, parsed.text);
        assertTrue(parsed.ongoing);
        assertEquals(42, parsed.id);
        assertTrue(WearableNotificationPayload.dataItemPathForKey(original.key).startsWith("/notification/"));
        assertFalse(WearableNotificationPayload.dataItemPathForKey(original.key).contains("0|"));
    }
}
