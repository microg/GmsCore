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
 * Report type defined for a {@link TemporaryExposureKey}.
 */
@PublicApi
public @interface ReportType {
    int UNKNOWN = 0;
    int CONFIRMED_TEST = 1;
    int CONFIRMED_CLINICAL_DIAGNOSIS = 2;
    int SELF_REPORT = 3;
    int RECURSIVE = 4;
    int REVOKED = 5;

    @PublicApi(exclude = true)
    int VALUES = 6;
}
