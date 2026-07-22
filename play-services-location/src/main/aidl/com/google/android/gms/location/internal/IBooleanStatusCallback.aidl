package com.google.android.gms.location.internal;

import com.google.android.gms.common.api.Status;

interface IBooleanStatusCallback {
    void onBooleanStatus(in Status status, boolean bool);
}
