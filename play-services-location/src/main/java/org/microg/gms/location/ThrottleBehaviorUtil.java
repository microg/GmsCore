/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location;

import androidx.annotation.NonNull;
import com.google.android.gms.location.ThrottleBehavior;

public class ThrottleBehaviorUtil {
    public static boolean isValidThrottleBehavior(@ThrottleBehavior int throttleBehavior) {
        switch (throttleBehavior) {
            default:
                return false;
            case ThrottleBehavior.THROTTLE_BACKGROUND:
            case ThrottleBehavior.THROTTLE_ALWAYS:
            case ThrottleBehavior.THROTTLE_NEVER:
                return true;
        }
    }

    public static int checkValidThrottleBehavior(@ThrottleBehavior int throttleBehavior) {
        if (!isValidThrottleBehavior(throttleBehavior)) {
            throw new IllegalArgumentException("throttle behavior " + throttleBehavior + " must be a ThrottleBehavior.THROTTLE_* constant");
        }
        return throttleBehavior;
    }

    @NonNull
    public static String throttleBehaviorToString(@ThrottleBehavior int throttleBehavior) {
        switch (throttleBehavior) {
            case ThrottleBehavior.THROTTLE_BACKGROUND:
                return "THROTTLE_BACKGROUND";
            case ThrottleBehavior.THROTTLE_ALWAYS:
                return "THROTTLE_ALWAYS";
            case ThrottleBehavior.THROTTLE_NEVER:
                return "THROTTLE_NEVER";
            default:
                throw new IllegalArgumentException();
        }
    }
}
