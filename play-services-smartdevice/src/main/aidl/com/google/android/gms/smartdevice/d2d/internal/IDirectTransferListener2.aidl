/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.smartdevice.d2d.internal;

import android.app.PendingIntent;
interface IDirectTransferListener2 {
    void onUserActionRequired(in PendingIntent pendingIntent) = 3;
    void onTransferError(int errorCode, String description) = 4;
}
