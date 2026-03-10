package com.google.android.gms.auth.account.data;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.auth.GetHubTokenInternalResponse;

interface IGetHubTokenCallback {
    void onGetHubTokenResponse(in Status status, in GetHubTokenInternalResponse bundle) = 1;
}