/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location;

import androidx.annotation.NonNull;
import com.google.android.gms.location.Priority;

public class PriorityUtil {
    public static boolean isValidPriority(@Priority int priority) {
        switch (priority) {
            default:
                return false;
            case Priority.PRIORITY_HIGH_ACCURACY:
            case Priority.PRIORITY_BALANCED_POWER_ACCURACY:
            case Priority.PRIORITY_LOW_POWER:
            case Priority.PRIORITY_PASSIVE:
                return true;
        }
    }

    public static int checkValidPriority(@Priority int priority) {
        if (!isValidPriority(priority)) {
            throw new IllegalArgumentException("priority " + priority + " must be a Priority.PRIORITY_* constant");
        }
        return priority;
    }

    @NonNull
    public static String priorityToString(@Priority int priority) {
        switch (priority) {
            case Priority.PRIORITY_HIGH_ACCURACY:
                return "HIGH_ACCURACY";
            case Priority.PRIORITY_BALANCED_POWER_ACCURACY:
                return "BALANCED_POWER_ACCURACY";
            case Priority.PRIORITY_LOW_POWER:
                return "LOW_POWER";
            case Priority.PRIORITY_PASSIVE:
                return "PASSIVE";
            default:
                throw new IllegalArgumentException();
        }
    }
}
