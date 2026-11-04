package com.google.android.gms.cast.framework.media.internal;

interface IFetchBitmapTaskProgressPublisher {
    void publishProgress(long bytesDownloaded, long bytesTotal) = 0;
    int getSupportedVersion() = 1;
}
