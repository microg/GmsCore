/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.nearby.exposurenotification;

import com.google.android.gms.common.api.CommonStatusCodes;

public class ExposureNotificationStatusCodes extends CommonStatusCodes {
    public static final int FAILED = 13;
    public static final int FAILED_ALREADY_STARTED = 39500;
    public static final int FAILED_NOT_SUPPORTED = 39501;
    public static final int FAILED_REJECTED_OPT_IN = 39502;
    public static final int FAILED_SERVICE_DISABLED = 39503;
    public static final int FAILED_BLUETOOTH_DISABLED = 39504;
    public static final int FAILED_TEMPORARILY_DISABLED = 39505;
    public static final int FAILED_DISK_IO = 39506;
    public static final int FAILED_UNAUTHORIZED = 39507;

    public static String getStatusCodeString(final int statusCode) {
        switch (statusCode) {
            case FAILED_ALREADY_STARTED:
                return "FAILED_ALREADY_STARTED";
            case FAILED_NOT_SUPPORTED:
                return "FAILED_NOT_SUPPORTED";
            case FAILED_REJECTED_OPT_IN:
                return "FAILED_REJECTED_OPT_IN";
            case FAILED_SERVICE_DISABLED:
                return "FAILED_SERVICE_DISABLED";
            case FAILED_BLUETOOTH_DISABLED:
                return "FAILED_BLUETOOTH_DISABLED";
            case FAILED_TEMPORARILY_DISABLED:
                return "FAILED_TEMPORARILY_DISABLED";
            case FAILED_DISK_IO:
                return "FAILED_DISK_IO";
            case FAILED_UNAUTHORIZED:
                return "FAILED_UNAUTHORIZED";
            default:
                return CommonStatusCodes.getStatusCodeString(statusCode);
        }
    }
}
