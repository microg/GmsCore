/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.nearby.exposurenotification;

import org.microg.gms.common.PublicApi;

import java.util.HashSet;
import java.util.Set;

/**
 * Detail status for exposure notification service.
 */
@PublicApi
public enum ExposureNotificationStatus {
    /**
     * Exposure notification is running.
     */
    ACTIVATED,
    /**
     * Exposure notification is not running.
     */
    INACTIVATED,
    /**
     * Bluetooth is not enabled.
     */
    BLUETOOTH_DISABLED,
    /**
     * Location is not enabled.
     */
    LOCATION_DISABLED,
    /**
     * User is not consent for the client.
     */
    NO_CONSENT,
    /**
     * The client is not in approved client list.
     */
    NOT_IN_WHITELIST,
    /**
     * Can't detected the BLE supporting of this device due to bluetooth is not enabled.
     */
    BLUETOOTH_SUPPORT_UNKNOWN,
    /**
     * Hardware of this device doesn't support exposure notification.
     */
    HW_NOT_SUPPORT,
    /**
     * There is another client running as active client.
     */
    FOCUS_LOST,
    /**
     * Device storage is not sufficient for exposure notification.
     */
    LOW_STORAGE,
    /**
     * Current status is unknown.
     */
    UNKNOWN,
    /**
     * Exposure notification is not supported.
     */
    EN_NOT_SUPPORT,
    /**
     * Exposure notification is not supported for current user profile.
     */
    USER_PROFILE_NOT_SUPPORT
    ;

    private long flag() {
        return 1 << ordinal();
    }

    @PublicApi(exclude = true)
    public static long setToFlags(Set<ExposureNotificationStatus> set) {
        long res = 0;
        for (ExposureNotificationStatus status : set) {
            res |= status.flag();
        }
        return res;
    }

    @PublicApi(exclude = true)
    public static Set<ExposureNotificationStatus> flagsToSet(long flags) {
        Set<ExposureNotificationStatus> set = new HashSet<>();
        for (ExposureNotificationStatus status : values()) {
            if ((flags & status.flag()) > 0) {
                set.add(status);
            }
        }
        return set;
    }
}
