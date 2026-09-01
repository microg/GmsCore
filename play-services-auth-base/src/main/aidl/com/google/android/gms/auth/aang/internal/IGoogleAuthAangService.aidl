package com.google.android.gms.auth.aang.internal;

import com.google.android.gms.auth.aang.GetAccountsRequest;
import com.google.android.gms.auth.aang.HasCapabilitiesRequest;
import com.google.android.gms.auth.aang.internal.IGoogleAuthAangCallbacks;

interface IGoogleAuthAangService {
    void getAccounts(IGoogleAuthAangCallbacks callback, in GetAccountsRequest request) = 0;
    void hasCapabilities(IGoogleAuthAangCallbacks callback, in HasCapabilitiesRequest request) = 3;
}
