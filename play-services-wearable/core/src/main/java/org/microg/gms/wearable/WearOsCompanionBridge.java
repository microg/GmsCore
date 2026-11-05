/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import android.os.Bundle;

/**
 * Helper for WearOS companion pairing capabilities, phone notification echoing,
 * and media control path routing (#2843).
 */
public final class WearOsCompanionBridge {
    public static final String PATH_NOTIFICATION_ECHO = "/wearos/notification/echo";
    public static final String PATH_MEDIA_CONTROL = "/wearos/media/control";
    public static final String CAPABILITY_WEAR_COMPANION = "wear_os_companion";

    private WearOsCompanionBridge() {
    }

    public static Bundle createNotificationEchoBundle(
            String packageName,
            int notificationId,
            String title,
            String text,
            long timestampMs
    ) {
        Bundle bundle = new Bundle();
        bundle.putString("package_name", packageName != null ? packageName : "");
        bundle.putInt("notification_id", notificationId);
        bundle.putString("title", title != null ? title : "");
        bundle.putString("text", text != null ? text : "");
        bundle.putLong("timestamp_ms", timestampMs);
        return bundle;
    }

    public static boolean isSupportedWearCommandPath(String path) {
        return PATH_NOTIFICATION_ECHO.equals(path) || PATH_MEDIA_CONTROL.equals(path);
    }
}
