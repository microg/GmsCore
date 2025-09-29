/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.locationsharingreporter.service

enum class LocationShareIssue(val code: Int) {

    UNKNOWN(0),
    SHARE_ENDED(1),
    UPLOAD_REQUEST_EXPIRED(2),
    CLIENT_PERMISSION_LOST(3),
    BATTERY_SAVER_ENABLED(4),
    NOT_PRIMARY_DEVICE(5),
    LOCATION_DISABLED_IN_SETTINGS(6),
    CLIENT_NO_LOCATION_ACCESS(7),
    NOT_SHARING_LOCATION(8),
    INELIGIBLE(9),
    MALFORMED_LOCATION_SHARE(10),
    ACCOUNT_REMOVED(11),
    NOT_SERVING_LOCATIONS(12),
    SHARING_DISABLED(13),
    CENTRAL_NOTICE_NOT_ACKED(14);
}