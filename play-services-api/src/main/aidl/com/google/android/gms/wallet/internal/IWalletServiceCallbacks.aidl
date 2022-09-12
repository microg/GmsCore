package com.google.android.gms.wallet.internal;

import com.google.android.gms.common.api.Status;

interface IWalletServiceCallbacks {
    void onIsReadyToPayResponse(in Status status, boolean result, in Bundle args) = 8;
}
