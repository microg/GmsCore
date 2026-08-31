/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import com.google.android.gms.wearable.ConnectionConfiguration;

import org.microg.gms.wearable.bluetooth.WearableBtUuids;

/**
 * Classifies {@link ConnectionConfiguration} rows so WearableImpl can start the matching transport.
 * <p>
 * GMS uses {@code type=1} for Bluetooth Classic (Wear OS companion), {@code type=3} for the
 * local TCP debug socket, and a Bluetooth MAC in {@code address} even when type is omitted.
 */
public final class WearableConnectionKind {
    public static final int TYPE_BLUETOOTH = 1;
    public static final int TYPE_CLOUD = 2;
    public static final int TYPE_NETWORK = 3;

    private WearableConnectionKind() {
    }

    public static boolean isBluetoothAddress(String address) {
        return WearableBtUuids.isBluetoothAddress(address);
    }

    public static boolean isBluetooth(ConnectionConfiguration config) {
        if (config == null) {
            return false;
        }
        if (config.type == TYPE_BLUETOOTH) {
            return true;
        }
        return isBluetoothAddress(config.address);
    }

    public static boolean isTcpServer(ConnectionConfiguration config) {
        if (config == null) {
            return false;
        }
        return "server".equals(config.name) || config.type == TYPE_NETWORK;
    }
}
