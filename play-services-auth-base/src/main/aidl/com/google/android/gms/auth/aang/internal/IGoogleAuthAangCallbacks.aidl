package com.google.android.gms.auth.aang.internal;

import com.google.android.gms.auth.aang.GetAccountsResponse;
import com.google.android.gms.common.api.Status;

interface IGoogleAuthAangCallbacks {
    void onGetAccounts(in Status status, in GetAccountsResponse response) = 0;
    void onHasCapabilities(in Status status, int result) = 2;
}
