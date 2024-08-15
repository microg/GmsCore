/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.vending.billing;

import android.os.Bundle;


interface IInAppBillingGetBillingConfigCallback {
    void callback(in Bundle bundle);
}