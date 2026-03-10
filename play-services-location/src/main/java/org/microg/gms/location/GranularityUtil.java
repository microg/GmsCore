/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location;

import com.google.android.gms.location.Granularity;
import com.google.android.gms.location.Priority;

public class GranularityUtil {
    public static boolean isValidGranularity(int granularity) {
        switch (granularity) {
            default:
                return false;
            case Granularity.GRANULARITY_PERMISSION_LEVEL:
            case Granularity.GRANULARITY_COARSE:
            case Granularity.GRANULARITY_FINE:
                return true;
        }
    }

    public static int checkValidGranularity(int granularity) {
        if (!isValidGranularity(granularity)) {
            throw new IllegalArgumentException("granularity " + granularity + " must be a Granularity.GRANULARITY_* constant");
        }
        return granularity;
    }

    public static String granularityToString(int granularity) {
        switch (granularity) {
            case Granularity.GRANULARITY_PERMISSION_LEVEL:
                return "GRANULARITY_PERMISSION_LEVEL";
            case Granularity.GRANULARITY_COARSE:
                return "GRANULARITY_COARSE";
            case Granularity.GRANULARITY_FINE:
                return "GRANULARITY_FINE";
            default:
                throw new IllegalArgumentException();
        }
    }
}
