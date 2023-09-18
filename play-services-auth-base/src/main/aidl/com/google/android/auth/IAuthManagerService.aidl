package com.google.android.auth;

import android.os.Bundle;
import android.accounts.Account;

import com.google.android.gms.auth.AccountChangeEventsResponse;
import com.google.android.gms.auth.AccountChangeEventsRequest;
import com.google.android.gms.auth.GetHubTokenRequest;
import com.google.android.gms.auth.GetHubTokenInternalResponse;
import com.google.android.gms.auth.HasCapabilitiesRequest;

interface IAuthManagerService {
    Bundle getToken(String accountName, String scope, in Bundle extras) = 0;
    Bundle clearToken(String token, in Bundle extras) = 1;
    AccountChangeEventsResponse getChangeEvents(in AccountChangeEventsRequest request) = 2;

    Bundle getTokenWithAccount(in Account account, String scope, in Bundle extras) = 4;
    Bundle getAccounts(in Bundle extras) = 5;
    Bundle removeAccount(in Account account) = 6;
    Bundle requestGoogleAccountsAccess(String packageName) = 7;
    int hasCapabilities(in HasCapabilitiesRequest request) = 8;
    GetHubTokenInternalResponse getHubToken(in GetHubTokenRequest request, in Bundle extras) = 9;
}
