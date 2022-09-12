package com.google.android.gms.cast.framework;

import com.google.android.gms.dynamic.IObjectWrapper;

interface ISessionProxy {
    IObjectWrapper getWrappedSession() = 0;
    void start(in Bundle extras) = 1;
    void resume(in Bundle extras) = 2;
    void end(boolean paramBoolean) = 3;
    long getSessionRemainingTimeMs() = 4;
    int getSupportedVersion() = 5;
    void onStarting(in Bundle routeInfoExtra) = 6;
    void onResuming(in Bundle routeInfoExtra) = 7;
}
