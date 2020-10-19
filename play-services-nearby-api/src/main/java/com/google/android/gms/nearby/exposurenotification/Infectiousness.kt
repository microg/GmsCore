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
 * Infectiousness defined for an [ExposureWindow].
 */
@PublicApi
annotation class Infectiousness {
    companion object {
        const val NONE = 0
        const val STANDARD = 1
        const val HIGH = 2

        @PublicApi(exclude = true)
        const val VALUES = 3
    }
}