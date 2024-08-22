/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.vending.billing;

import android.os.Bundle;

interface IInAppBillingGetBillingConfigCallback {
    /**
     * @param bundle a Bundle with the following keys:
     *        "BILLING_CONFIG" - String with JSON encoded billing config with following keys:
     *            "countryCode" - String with customer's country code (ISO-3166-1 alpha2)
     */
    void callback(in Bundle bundle);
}