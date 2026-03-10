/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.vending.billing;

import android.os.Bundle;

interface IInAppBillingGetExternalPaymentDialogIntentCallback {
    /**
     * @param bundle a Bundle with the following keys:
     *        "RESPONSE_CODE" - Integer
     *        "DEBUG_MESSAGE" - String
     *        "EXTERNAL_PAYMENT_DIALOG_INTENT" - PendingIntent
     */
    void callback(in Bundle bundle);
}