/*
 * SPDX-FileCopyrightText: 2021 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.safetynet;

import org.microg.gms.common.PublicApi;

/**
 * Constants pertaining to the Verify Apps SafetyNet API.
 */
@PublicApi
public class VerifyAppsConstants {
    /**
     * An action that is broadcasted when harmful apps are discovered.
     */
    public static final String ACTION_HARMFUL_APPS_FOUND = "com.google.android.gms.safetynet.action.HARMFUL_APPS_FOUND";
    /**
     * An action that is broadcasted when a harmful app is blocked from installation.
     */
    public static final String ACTION_HARMFUL_APP_BLOCKED = "com.google.android.gms.safetynet.action.HARMFUL_APP_BLOCKED";
    /**
     * An action that is broadcasted when a harmful app is installed.
     */
    public static final String ACTION_HARMFUL_APP_INSTALLED = "com.google.android.gms.safetynet.action.HARMFUL_APP_INSTALLED";

    public static final int HARMFUL_CATEGORY_RANSOMWARE = 1;
    public static final int HARMFUL_CATEGORY_PHISHING = 2;
    public static final int HARMFUL_CATEGORY_TROJAN = 3;
    public static final int HARMFUL_CATEGORY_UNCOMMON = 4;
    public static final int HARMFUL_CATEGORY_FRAUDWARE = 5;
    public static final int HARMFUL_CATEGORY_TOLL_FRAUD = 6;
    public static final int HARMFUL_CATEGORY_WAP_FRAUD = 7;
    public static final int HARMFUL_CATEGORY_CALL_FRAUD = 8;
    public static final int HARMFUL_CATEGORY_BACKDOOR = 9;
    public static final int HARMFUL_CATEGORY_SPYWARE = 10;
    public static final int HARMFUL_CATEGORY_GENERIC_MALWARE = 11;
    public static final int HARMFUL_CATEGORY_HARMFUL_SITE = 12;
    public static final int HARMFUL_CATEGORY_WINDOWS_MALWARE = 13;
    public static final int HARMFUL_CATEGORY_HOSTILE_DOWNLOADER = 14;
    public static final int HARMFUL_CATEGORY_NON_ANDROID_THREAT = 15;
    public static final int HARMFUL_CATEGORY_ROOTING = 16;
    public static final int HARMFUL_CATEGORY_PRIVILEGE_ESCALATION = 17;
    public static final int HARMFUL_CATEGORY_TRACKING = 18;
    public static final int HARMFUL_CATEGORY_SPAM = 19;
    public static final int HARMFUL_CATEGORY_DENIAL_OF_SERVICE = 20;
    public static final int HARMFUL_CATEGORY_DATA_COLLECTION = 21;
}
