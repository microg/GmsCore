/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification;

import static org.microg.gms.common.Constants.GMS_VERSION_CODE;

public class Constants {
    public static final String ACTION_EXPOSURE_NOTIFICATION_SETTINGS = "com.google.android.gms.settings.EXPOSURE_NOTIFICATION_SETTINGS";
    public static final String ACTION_EXPOSURE_NOT_FOUND = "com.google.android.gms.exposurenotification.ACTION_EXPOSURE_NOT_FOUND";
    public static final String ACTION_EXPOSURE_STATE_UPDATED = "com.google.android.gms.exposurenotification.ACTION_EXPOSURE_STATE_UPDATED";
    public static final String ACTION_PRE_AUTHORIZE_RELEASE_PHONE_UNLOCKED = "com.google.android.gms.exposurenotification.ACTION_PRE_AUTHORIZE_RELEASE_PHONE_UNLOCKED";
    public static final String ACTION_SERVICE_STATE_UPDATED = "com.google.android.gms.exposurenotification.ACTION_SERVICE_STATE_UPDATED";
    public static final String EXTRA_EXPOSURE_SUMMARY = "com.google.android.gms.exposurenotification.EXTRA_EXPOSURE_SUMMARY";
    public static final String EXTRA_SERVICE_STATE = "com.google.android.gms.exposurenotification.EXTRA_SERVICE_STATE";
    public static final String EXTRA_TEMPORARY_EXPOSURE_KEY_LIST = "com.google.android.gms.exposurenotification.EXTRA_TEMPORARY_EXPOSURE_KEY_LIST";
    public static final String EXTRA_TOKEN = "com.google.android.gms.exposurenotification.EXTRA_TOKEN";
    public static final String TOKEN_A = "TYZWQ32170AXEUVCDW7A";
    public static final int DAYS_SINCE_ONSET_OF_SYMPTOMS_UNKNOWN = Integer.MAX_VALUE;
    public static final int VERSION = 18;
    public static final long VERSION_FULL = ((long)VERSION) * 1000000000L + GMS_VERSION_CODE;
}
