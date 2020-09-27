/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.nearby.exposurenotification;

import org.microg.gms.common.PublicApi;

/**
 * Infectiousness defined for an {@link ExposureWindow}.
 */
@PublicApi
public @interface Infectiousness {
    int NONE = 0;
    int STANDARD = 1;
    int HIGH = 2;

    @PublicApi(exclude = true)
    int VALUES = 3;
}
