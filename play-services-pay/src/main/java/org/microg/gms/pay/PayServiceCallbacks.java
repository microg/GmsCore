/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.pay;

import android.app.PendingIntent;
import android.os.RemoteException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.pay.EmoneyReadiness;
import com.google.android.gms.pay.GetSortOrderResponse;
import com.google.android.gms.pay.PayApiError;
import com.google.android.gms.pay.ProtoSafeParcelable;
import com.google.android.gms.pay.internal.IPayServiceCallbacks;

public class PayServiceCallbacks extends IPayServiceCallbacks.Stub {
    @Override
    public void onStatus(Status status) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onPendingIntent(Status status, PendingIntent pendingIntent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onProtoSafeParcelable(Status status, ProtoSafeParcelable proto) throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onGetSortOrderResponse(Status status, GetSortOrderResponse response) throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onBoolean(Status status, boolean b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onPayApiError(PayApiError error) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onByteArray(Status status, byte[] bArr) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onLong(Status status, long l) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onError(Status status) {
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
