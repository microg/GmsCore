package com.google.android.auth;

import android.os.Bundle;

import com.google.android.gms.auth.AccountChangeEventsResponse;
import com.google.android.gms.auth.AccountChangeEventsRequest;

interface IAuthManagerService {
    Bundle getToken(String accountName, String scope, in Bundle extras);
    Bundle clearToken(String token, in Bundle extras);
    AccountChangeEventsResponse getChangeEvents(in AccountChangeEventsRequest request);
}
