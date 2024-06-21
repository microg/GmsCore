package com.google.android.gms.pay.internal;

import android.app.PendingIntent;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.pay.GetWalletStatusResponse;


interface IPayServiceCallbacks {
    void onStatusResult(in Status status, in byte[] bArr) = 14;

    void onSavePasses(in Status status) = 17;

    void onGetPayApiAvailabilityStatus(in Status status, int i) = 19;

    void onGetPendingIntent(in Status status, in PendingIntent pendingIntent) = 3;

    void onError(in Status status) = 1;

    void onGetWalletStatus(in Status status, in GetWalletStatusResponse getWalletStatusResponse) = 21;

}