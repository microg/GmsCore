package com.google.android.gms.wallet.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.wallet.GetClientTokenResponse;

interface IWalletServiceCallbacks {
    void onIsReadyToPayResponse(in Status status, boolean result, in Bundle args) = 8;
    void onClientTokenReceived(in Status status, in GetClientTokenResponse response, in Bundle extras) = 9;
}
