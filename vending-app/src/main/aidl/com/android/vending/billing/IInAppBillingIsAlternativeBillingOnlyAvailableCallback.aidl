/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.vending.billing;

import android.os.Bundle;

interface IInAppBillingIsAlternativeBillingOnlyAvailableCallback {
    /**
     * @param bundle a Bundle with the following keys:
     *        "RESPONSE_CODE" - Integer
     *        "DEBUG_MESSAGE" - String
     */
    void callback(in Bundle bundle);
}