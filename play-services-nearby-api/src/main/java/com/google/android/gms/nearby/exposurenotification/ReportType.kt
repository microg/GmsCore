/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */
package com.google.android.gms.nearby.exposurenotification

import org.microg.gms.common.PublicApi

/**
 * Report type defined for a [TemporaryExposureKey].
 */
@PublicApi
annotation class ReportType {
    companion object {
        const val UNKNOWN = 0
        const val CONFIRMED_TEST = 1
        const val CONFIRMED_CLINICAL_DIAGNOSIS = 2
        const val SELF_REPORT = 3
        const val RECURSIVE = 4
        const val REVOKED = 5

        @PublicApi(exclude = true)
        const val VALUES = 6
    }
}