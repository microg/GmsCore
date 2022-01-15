/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.tapandpay;

import com.google.android.gms.common.api.CommonStatusCodes;

import org.microg.gms.common.PublicApi;

@PublicApi
public class TapAndPayStatusCodes extends CommonStatusCodes {
    public static final int TAP_AND_PAY_NO_ACTIVE_WALLET = 15002;
    public static final int TAP_AND_PAY_TOKEN_NOT_FOUND = 15003;
    public static final int TAP_AND_PAY_INVALID_TOKEN_STATE = 15004;
    public static final int TAP_AND_PAY_ATTESTATION_ERROR = 15005;
    public static final int TAP_AND_PAY_UNAVAILABLE = 15009;
}
