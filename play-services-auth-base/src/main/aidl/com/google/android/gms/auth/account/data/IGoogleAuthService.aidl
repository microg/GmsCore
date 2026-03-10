package com.google.android.gms.auth.account.data;

import android.accounts.Account;

import com.google.android.gms.auth.AccountChangeEventsRequest;
import com.google.android.gms.auth.GetAccountsRequest;
import com.google.android.gms.auth.GetHubTokenRequest;
import com.google.android.gms.auth.HasCapabilitiesRequest;
import com.google.android.gms.auth.account.data.IBundleCallback;
import com.google.android.gms.auth.account.data.IGetAccountChangeEventsCallback;
import com.google.android.gms.auth.account.data.IGetAccountsCallback;
import com.google.android.gms.auth.account.data.IGetHubTokenCallback;
import com.google.android.gms.auth.account.data.IGetTokenWithDetailsCallback;
import com.google.android.gms.auth.account.data.IHasCapabilitiesCallback;
import com.google.android.gms.auth.firstparty.dataservice.ClearTokenRequest;
import com.google.android.gms.common.api.internal.IStatusCallback;

interface IGoogleAuthService {
    void getTokenWithDetails(IGetTokenWithDetailsCallback callback, in Account account, String service, in Bundle extras) = 0;
    void clearToken(IStatusCallback callback, in ClearTokenRequest request) = 1;
    void requestAccountsAccess(IBundleCallback callback, String str) = 2;
    void getAccountChangeEvents(IGetAccountChangeEventsCallback callback, in AccountChangeEventsRequest request) = 3;
    void getAccounts(IGetAccountsCallback callback, in GetAccountsRequest request) = 4;
    void removeAccount(IBundleCallback callback, in Account account) = 5;
    void hasCapabilities(IHasCapabilitiesCallback callback, in HasCapabilitiesRequest request) = 6;
    void getHubToken(IGetHubTokenCallback callback, in GetHubTokenRequest request) = 7;
}