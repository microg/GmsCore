package com.google.android.gms.wallet.internal;

import com.google.android.gms.wallet.internal.IWalletServiceCallbacks;
import com.google.android.gms.wallet.IsReadyToPayRequest;

interface IOwService {
    void isReadyToPay(in IsReadyToPayRequest request, in Bundle args, IWalletServiceCallbacks callbacks) = 13;
}
