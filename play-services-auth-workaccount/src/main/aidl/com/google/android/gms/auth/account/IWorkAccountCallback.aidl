package com.google.android.gms.auth.account;

import android.accounts.Account;

interface IWorkAccountCallback {
    void onAccountAdded(in Account account) = 0;
    void onAccountRemoved(boolean success) = 1;
}