/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.libraries.entitlement.utils;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Defines the constants used for TS43 operations. */
public final class Ts43Constants {
    /** App ID unknown. For initialization only. */
    public static final String APP_UNKNOWN = "";

    /** App ID for Voice-Over-LTE entitlement. */
    public static final String APP_VOLTE = "ap2003";

    /** App ID for Voice-Over-WiFi entitlement. */
    public static final String APP_VOWIFI = "ap2004";

    /** App ID for SMS-Over-IP entitlement. */
    public static final String APP_SMSOIP = "ap2005";

    /** App ID for on device service activation (ODSA) for companion device. */
    public static final String APP_ODSA_COMPANION = "ap2006";

    /** App ID for on device service activation (ODSA) for primary device. */
    public static final String APP_ODSA_PRIMARY = "ap2009";

    /** App ID for data plan information entitlement. */
    public static final String APP_DATA_PLAN_BOOST = "ap2010";

    /** App ID for server initiated requests, entitlement and activation. */
    public static final String APP_ODSA_SERVER_INITIATED_REQUESTS = "ap2011";

    /** App ID for direct carrier billing. */
    public static final String APP_DIRECT_CARRIER_BILLING = "ap2012";

    /** App ID for private user identity. */
    public static final String APP_PRIVATE_USER_IDENTITY = "ap2013";

    /** App ID for phone number information. */
    public static final String APP_PHONE_NUMBER_INFORMATION = "ap2014";

    /** App ID for satellite entitlement. */
    public static final String APP_SATELLITE_ENTITLEMENT = "ap2016";

    /** App ID for ODSA for Cross-TS.43 platform device, Entitlement and Activation */
    public static final String APP_ODSA_CROSS_TS43 = "ap2017";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            APP_UNKNOWN,
            APP_VOLTE,
            APP_VOWIFI,
            APP_SMSOIP,
            APP_ODSA_COMPANION,
            APP_ODSA_PRIMARY,
            APP_DATA_PLAN_BOOST,
            APP_ODSA_SERVER_INITIATED_REQUESTS,
            APP_DIRECT_CARRIER_BILLING,
            APP_PRIVATE_USER_IDENTITY,
            APP_PHONE_NUMBER_INFORMATION,
            APP_SATELLITE_ENTITLEMENT,
            APP_ODSA_CROSS_TS43
    })
    public @interface AppId {
    }

    /**
     * Check if the application id is valid.
     *
     * @param appId The application id.
     * @return {@code true} if valid, otherwise {@code false}.
     */
    public static boolean isValidAppId(@NonNull @AppId String appId) {
        switch (appId) {
            case APP_VOLTE:
            case APP_VOWIFI:
            case APP_SMSOIP:
            case APP_ODSA_COMPANION:
            case APP_ODSA_PRIMARY:
            case APP_DATA_PLAN_BOOST:
            case APP_ODSA_SERVER_INITIATED_REQUESTS:
            case APP_DIRECT_CARRIER_BILLING:
            case APP_PRIVATE_USER_IDENTITY:
            case APP_PHONE_NUMBER_INFORMATION:
            case APP_SATELLITE_ENTITLEMENT:
            case APP_ODSA_CROSS_TS43:
                return true;
            default: // fall through
        }
        return false;
    }

    /**
     * Action to disable notification token.
     */
    public static final int NOTIFICATION_ACTION_DISABLE = 0;

    /**
     * Action to enable GCM notification token.
     */
    public static final int NOTIFICATION_ACTION_ENABLE_GCM = 1;

    /**
     * Action to enable FCM notification token.
     */
    public static final int NOTIFICATION_ACTION_ENABLE_FCM = 2;

    /**
     * Action to enable WNS push notification token.
     */
    public static final int NOTIFICATION_ACTION_ENABLE_WNS = 3;

    /**
     * Action to enable APNS notification token.
     */
    public static final int NOTIFICATION_ACTION_ENABLE_APNS = 4;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            NOTIFICATION_ACTION_DISABLE,
            NOTIFICATION_ACTION_ENABLE_GCM,
            NOTIFICATION_ACTION_ENABLE_FCM,
            NOTIFICATION_ACTION_ENABLE_WNS,
            NOTIFICATION_ACTION_ENABLE_APNS,
    })
    public @interface NotificationAction {}

    /**
     * Check if the notification action is valid.
     *
     * @param notificationAction The notification action.
     * @return {@code true} if valid, otherwise {@code false}.
     */
    public static boolean isValidNotificationAction(@NotificationAction int notificationAction) {
        switch (notificationAction) {
            case NOTIFICATION_ACTION_DISABLE:
            case NOTIFICATION_ACTION_ENABLE_GCM:
            case NOTIFICATION_ACTION_ENABLE_FCM:
            case NOTIFICATION_ACTION_ENABLE_WNS:
            case NOTIFICATION_ACTION_ENABLE_APNS:
                return true;
            default: // fall through
        }
        return false;
    }

    /** Default entitlement version. */
    public static final String DEFAULT_ENTITLEMENT_VERSION = "2.0";

    private Ts43Constants() {
    }
}