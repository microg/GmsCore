/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.pay;

import android.app.PendingIntent;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.pay.EmoneyReadiness;
import com.google.android.gms.pay.PayApiError;
import com.google.android.gms.pay.internal.IPayServiceCallbacks;

public class PayServiceCallbacks extends IPayServiceCallbacks.Stub {
    @Override
    public void onStatus(Status status) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onPendingIntentForWalletOnWear(Status status, PendingIntent pendingIntent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onStatusAndBoolean(Status status, boolean b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onPayApiError(PayApiError error) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onStatusAndByteArray(Status status, byte[] bArr) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onStatusAndLong(Status status, long l) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onPendingIntent(Status status) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onPayApiAvailabilityStatus(Status status, int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onEmoneyReadiness(Status status, EmoneyReadiness emoneyReadiness) {
        throw new UnsupportedOperationException();
    }
}
