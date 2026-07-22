package com.google.android.gms.auth.api.phone.internal;

import com.google.android.gms.common.api.Status;

interface ISmsRetrieverResultCallback {
    void onResult(in Status status) = 0;
}
