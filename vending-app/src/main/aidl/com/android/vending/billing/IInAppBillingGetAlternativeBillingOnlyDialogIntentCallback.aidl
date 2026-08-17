/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.vending.billing;

import android.os.Bundle;

interface IInAppBillingGetAlternativeBillingOnlyDialogIntentCallback {
    /**
     * @param bundle a Bundle with the following keys:
     *        "RESPONSE_CODE" - Integer
     *        "DEBUG_MESSAGE" - String
     *        "ALTERNATIVE_BILLING_ONLY_DIALOG_INTENT" - PendingIntent
     */
    void callback(in Bundle bundle);
}