package com.google.android.gms.ads.internal.request;

import com.google.android.gms.ads.internal.ExceptionParcel;

interface INonagonStreamingResponseListener {
    void onSuccess(in ParcelFileDescriptor fd);
    void onException(in ExceptionParcel exception);
}