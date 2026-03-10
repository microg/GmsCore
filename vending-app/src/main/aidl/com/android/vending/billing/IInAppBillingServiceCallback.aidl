/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.vending.billing;

import android.os.Bundle;

interface IInAppBillingServiceCallback {
    /**
     * @param bundle a Bundle with the following keys:
     *        "KEY_LAUNCH_INTENT" - PendingIntent
     */
    void callback(in Bundle bundle);
}