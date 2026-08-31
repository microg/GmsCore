/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable.bluetooth;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * RFCOMM service UUIDs advertised by Wear OS / Android Wear companions.
 * <p>
 * Generic SPP ({@code 00001101-0000-1000-8000-00805F9B34FB}) is <em>not</em> used by Wear OS.
 * The watch looks up these named SDP records (see BluetoothSocket {@code mServiceName=WearableBt}
 * in pairing traces). Listen and connect with this set, in order.
 */
public final class WearableBtUuids {
    public static final UUID WEARABLE_BT = UUID.fromString("5e8945b0-9525-11e3-a5e2-0800200c9a66");
    public static final UUID FLOW = UUID.fromString("22b21d80-b0ae-11e3-9c1a-0800200c9a66");
    public static final UUID FLOW15 = UUID.fromString("ae3ead70-b0ae-11e3-9c1a-0800200c9a66");

    public static final String NAME_WEARABLE_BT = "WearableBt";
    public static final String NAME_FLOW = "Flow";
    public static final String NAME_FLOW15 = "Flow15";

    public static final List<NamedUuid> ALL = Collections.unmodifiableList(Arrays.asList(
            new NamedUuid(NAME_WEARABLE_BT, WEARABLE_BT),
            new NamedUuid(NAME_FLOW, FLOW),
            new NamedUuid(NAME_FLOW15, FLOW15)
    ));

    private static final Pattern BLUETOOTH_ADDRESS =
            Pattern.compile("^[0-9A-Fa-f]{2}(:[0-9A-Fa-f]{2}){5}$");

    private WearableBtUuids() {
    }

    public static boolean isBluetoothAddress(String address) {
        if (address == null || address.length() == 0 || "NULL_STRING".equals(address)) {
            return false;
        }
        return BLUETOOTH_ADDRESS.matcher(address).matches();
    }

    public static final class NamedUuid {
        public final String name;
        public final UUID uuid;

        public NamedUuid(String name, UUID uuid) {
            this.name = name;
            this.uuid = uuid;
        }
    }
}
