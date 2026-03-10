package com.google.android.gms.common.api.internal;

import com.google.android.gms.common.api.Status;

interface IStatusCallback {
    void onResult(in Status status);
}
