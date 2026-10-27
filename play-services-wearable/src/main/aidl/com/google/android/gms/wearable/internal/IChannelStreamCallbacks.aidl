package com.google.android.gms.wearable.internal;

interface IChannelStreamCallbacks {
    void onStreamClosed(int closeReason, int appSpecificErrorCode) = 1;
}
