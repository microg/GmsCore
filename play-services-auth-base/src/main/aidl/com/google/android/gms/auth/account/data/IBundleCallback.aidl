package com.google.android.gms.auth.account.data;

import com.google.android.gms.common.api.Status;

interface IBundleCallback {
    void onBundle(in Status status, in Bundle bundle) = 1;
}