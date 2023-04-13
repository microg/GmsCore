/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.nearby.exposurenotification;

import org.microg.gms.common.PublicApi;

/**
 * Risk level defined for an {@link TemporaryExposureKey}.
 */
@PublicApi
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

    @PublicApi(exclude = true)
    int VALUES = 9;
}
