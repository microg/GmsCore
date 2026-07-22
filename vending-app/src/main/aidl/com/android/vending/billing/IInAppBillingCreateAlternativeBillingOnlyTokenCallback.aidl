/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.vending.billing;

import android.os.Bundle;

interface IInAppBillingCreateAlternativeBillingOnlyTokenCallback {
    /**
     * @param bundle a Bundle with the following keys:
     *        "RESPONSE_CODE" - Integer
     *        "DEBUG_MESSAGE" - String
     *        "CREATE_ALTERNATIVE_BILLING_ONLY_REPORTING_DETAILS" - String with JSON encoded reporting details with the following keys:
     *            "externalTransactionToken" - String used to report a transaction made via alternative billing
     */
    void callback(in Bundle bundle);
}