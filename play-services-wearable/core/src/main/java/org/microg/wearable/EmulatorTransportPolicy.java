/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.wearable;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;

/**
 * The loopback/ADB transport is a development facility, not an authenticated wearable link.
 * A debug build on an emulator trusts the developer's host and the installed applications.
 */
public final class EmulatorTransportPolicy {
    private EmulatorTransportPolicy() { }

    public static void requireAllowed(Context context) {
        boolean debuggable = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        if (!debuggable || !isAllowed(true, readProperty("ro.boot.qemu"), readProperty("ro.kernel.qemu"))) {
            throw new SecurityException("Wearable TCP is restricted to debuggable emulator builds");
        }
    }

    static boolean isAllowed(boolean debuggable, String bootQemu, String legacyQemu) {
        if (!debuggable || bootQemu == null) return false;
        // A present modern property is authoritative; legacy is only for older emulator images.
        if (bootQemu != null && !bootQemu.isEmpty()) return "1".equals(bootQemu);
        return "1".equals(legacyQemu);
    }

    @SuppressLint("PrivateApi")
    private static String readProperty(String name) {
        // Use the same read-only SystemProperties access as DeviceSyncInfo. Do not use Build
        // fields: the device profile subsystem can substitute those within this process.
        try {
            Class<?> properties = Class.forName("android.os.SystemProperties");
            return (String) properties.getMethod("get", String.class, String.class).invoke(null, name, "");
        } catch (ReflectiveOperationException | RuntimeException error) {
            return null;
        }
    }
}
