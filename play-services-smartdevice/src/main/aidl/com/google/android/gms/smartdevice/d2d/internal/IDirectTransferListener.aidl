/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.smartdevice.d2d.internal;

import android.app.PendingIntent;
import com.google.android.gms.smartdevice.d2d.AccountTransferResult;

interface IDirectTransferListener {
    void onAccountsTransferred(in AccountTransferResult[] results) = 0;
    void onUserActionRequired(in PendingIntent pendingIntent) = 1;
    void onTransferError(int errorCode, String description) = 2;
}
