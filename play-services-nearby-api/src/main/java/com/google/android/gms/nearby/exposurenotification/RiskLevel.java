/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.exposurenotification;

public @interface RiskLevel {
    int RISK_LEVEL_INVALID = 0;
    int RISK_LEVEL_LOWEST = 1;
    int RISK_LEVEL_LOW = 2;
    int RISK_LEVEL_LOW_MEDIUM = 3;
    int RISK_LEVEL_MEDIUM = 4;
    int RISK_LEVEL_MEDIUM_HIGH = 5;
    int RISK_LEVEL_HIGH = 6;
    int RISK_LEVEL_VERY_HIGH = 7;
    int RISK_LEVEL_HIGHEST = 8;
}
