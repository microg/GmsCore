package com.google.android.gms.auth.account.data;

import com.google.android.gms.common.api.Status;

interface IGetAccountsCallback {
    void onBundle(in Status status, in List<Account> bundle) = 1;
}