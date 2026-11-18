package com.google.android.gms.cast.framework.media.internal;

interface IFetchBitmapTaskProgressPublisher {
    void publishProgress(long current, long total) = 0;
    int getSupportedVersion() = 1;
}
