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
 * Calibration confidence defined for an {@link ExposureWindow}.
 */
@PublicApi
public @interface CalibrationConfidence {
    /**
     * No calibration data, using fleet-wide as default options.
     */
    int LOWEST = 0;
    /**
     * Using average calibration over models from manufacturer.
     */
    int LOW = 1;
    /**
     * Using single-antenna orientation for a similar model.
     */
    int MEDIUM = 2;
    /**
     * Using significant calibration data for this model.
     */
    int HIGH = 3;

    @PublicApi(exclude = true)
    int VALUES = 4;
}
