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
    public static final int TAP_AND_PAY_SAVE_CARD_ERROR = 15019;
    public static final int TAP_AND_PAY_INELIGIBLE_FOR_TOKENIZATION = 15021;
    public static final int TAP_AND_PAY_TOKENIZATION_DECLINED = 15022;
    public static final int TAP_AND_PAY_CHECK_ELIGIBILITY_ERROR = 15023;
    public static final int TAP_AND_PAY_TOKENIZE_ERROR = 15024;
    public static final int TAP_AND_PAY_TOKEN_ACTIVATION_REQUIRED = 15025;
    public static final int TAP_AND_PAY_PAYMENT_CREDENTIALS_DELIVERY_TIMEOUT = 15026;
    public static final int TAP_AND_PAY_USER_CANCELED_FLOW = 15027;
    public static final int TAP_AND_PAY_ENROLL_FOR_VIRTUAL_CARDS_FAILED = 15028;
}
