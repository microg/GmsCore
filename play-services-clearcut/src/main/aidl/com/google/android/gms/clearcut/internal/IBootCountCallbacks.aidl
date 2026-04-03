package com.google.android.gms.clearcut.internal;

import com.google.android.gms.common.api.Status;

interface IBootCountCallbacks {
    void onBootCount(in Status status, int bootCount) = 0;
}
