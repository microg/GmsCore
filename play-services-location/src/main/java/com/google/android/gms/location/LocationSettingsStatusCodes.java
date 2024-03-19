/*
 * SPDX-FileCopyrightText: 2017 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;

import org.microg.gms.common.PublicApi;

/**
 * Location settings specific status codes, for use in {@link Status#getStatusCode()}
 */
@PublicApi
public class LocationSettingsStatusCodes extends CommonStatusCodes {
    /**
     * Location settings can't be changed to meet the requirements, no dialog pops up
     */
    public static final int SETTINGS_CHANGE_UNAVAILABLE = 8502;
}
