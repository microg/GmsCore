package com.google.android.gms.wallet.internal;

import com.google.android.gms.wallet.internal.IWalletServiceCallbacks;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.GetClientTokenRequest;

interface IOwService {
    void isReadyToPay(in IsReadyToPayRequest request, in Bundle args, IWalletServiceCallbacks callbacks) = 13;
    void getClientToken(in GetClientTokenRequest getClientTokenRequest, in Bundle options, IWalletServiceCallbacks callbacks) = 14;
}
