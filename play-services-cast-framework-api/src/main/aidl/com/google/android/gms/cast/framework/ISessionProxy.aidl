package com.google.android.gms.cast.framework;

import com.google.android.gms.dynamic.IObjectWrapper;

interface ISessionProxy {
    IObjectWrapper getWrappedThis() = 0;
    void start(in Bundle paramBundle) = 1;
    void resume(in Bundle paramBundle) = 2;
    void end(boolean paramBoolean) = 3;
    long getSessionRemainingTimeMs() = 4;
    int getSupportedVersion() = 5;
    void onStarting(in Bundle paramBundle) = 6;
    void onResuming(in Bundle paramBundle) = 7;
}
