package com.google.android.gms.wearable.internal;

interface IChannelStreamCallbacks {
    void onChannelClosed(int closeReason, int appSpecificErrorCode) = 2;
}
