package com.google.android.gms.clearcut.internal;

import com.google.android.gms.common.api.Status;

interface IBootCountCallbacks {
    void responseBootCountCallback(in Status status, int code) = 0;
}
