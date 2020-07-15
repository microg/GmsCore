package com.google.android.gms.clearcut.internal;

import com.google.android.gms.common.api.Status;

interface IClearcutLoggerCallbacks {
    void onStatus(in Status status) = 0;
}
