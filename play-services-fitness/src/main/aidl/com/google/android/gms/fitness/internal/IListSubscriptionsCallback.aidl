/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.internal;

import com.google.android.gms.fitness.result.ListSubscriptionsResult;

interface IListSubscriptionsCallback {
    void onListSubscriptions(in ListSubscriptionsResult listSubscriptionsResult) = 0;
}
