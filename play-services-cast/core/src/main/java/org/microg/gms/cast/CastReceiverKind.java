/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cast;

import com.google.android.gms.cast.CastDevice;

import java.net.InetAddress;

/**
 * Separates hardware Chromecasts from loopback/software receivers and Cast Nearby devices.
 * Hardware and loopback results must not be treated as interchangeable.
 */
final class CastReceiverKind {
    enum Kind {
        HARDWARE,
        LOOPBACK,
        NEARBY
    }

    private CastReceiverKind() {}

    static Kind of(CastDevice device) {
        if (device == null) return Kind.LOOPBACK;
        String deviceId = device.getDeviceId();
        if (deviceId != null && deviceId.startsWith("__cast_nearby__")) {
            return Kind.NEARBY;
        }
        return ofHost(device.getInetAddress());
    }

    static Kind ofHost(InetAddress host) {
        if (host == null || host.isLoopbackAddress() || host.isAnyLocalAddress()) {
            return Kind.LOOPBACK;
        }
        return Kind.HARDWARE;
    }

    static boolean isHardware(CastDevice device) {
        return of(device) == Kind.HARDWARE;
    }

    static boolean isLoopback(CastDevice device) {
        return of(device) == Kind.LOOPBACK;
    }
}
