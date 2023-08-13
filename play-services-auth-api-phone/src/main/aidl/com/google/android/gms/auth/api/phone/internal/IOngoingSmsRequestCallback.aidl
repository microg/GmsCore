package com.google.android.gms.auth.api.phone.internal;

import com.google.android.gms.common.api.Status;

interface IOngoingSmsRequestCallback {
    void onHasOngoingSmsRequestResult(in Status status, boolean hasOngoingSmsRequest) = 0;
}
