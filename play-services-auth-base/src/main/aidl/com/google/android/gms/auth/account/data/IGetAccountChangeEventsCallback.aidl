package com.google.android.gms.auth.account.data;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.auth.AccountChangeEventsResponse;

interface IGetAccountChangeEventsCallback {
    void onAccountChangeEventsResponse(in Status status, in AccountChangeEventsResponse response) = 1;
}