package com.google.android.gms.auth.account;

import android.accounts.Account;
import com.google.android.gms.dynamic.IObjectWrapper;

interface IWorkAccountService {

    interface AddAccountResult {
        Account getAccount();
        IObjectWrapper getStatus();
    }

    void setWorkAuthenticatorEnabled(IObjectWrapper googleApiClient, boolean b);

    AddAccountResult addWorkAccount(IObjectWrapper googleApiClient, String s);

    IObjectWrapper removeWorkAccount(IObjectWrapper googleApiClient, IObjectWrapper account);


    IObjectWrapper setWorkAuthenticatorEnabledWithResult(IObjectWrapper googleApiClient, boolean b);
}