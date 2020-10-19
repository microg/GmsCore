/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */
package com.google.android.gms.nearby.exposurenotification

import org.microg.gms.common.PublicApi

/**
 * Risk level defined for an [TemporaryExposureKey].
 */
@PublicApi
annotation class RiskLevel {
    companion object {
        const val RISK_LEVEL_INVALID = 0
        const val RISK_LEVEL_LOWEST = 1
        const val RISK_LEVEL_LOW = 2
        const val RISK_LEVEL_LOW_MEDIUM = 3
        const val RISK_LEVEL_MEDIUM = 4
        const val RISK_LEVEL_MEDIUM_HIGH = 5
        const val RISK_LEVEL_HIGH = 6
        const val RISK_LEVEL_VERY_HIGH = 7
        const val RISK_LEVEL_HIGHEST = 8

        @PublicApi(exclude = true)
        const val VALUES = 9
    }
}