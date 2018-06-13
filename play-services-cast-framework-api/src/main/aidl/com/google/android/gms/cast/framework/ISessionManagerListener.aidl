package com.google.android.gms.cast.framework;

import com.google.android.gms.dynamic.IObjectWrapper;

interface ISessionManagerListener {
    IObjectWrapper getWrappedThis() = 0;
    void onSessionStarting(IObjectWrapper session) = 1;
    void onSessionStartFailed(IObjectWrapper session, int error) = 2;
    void onSessionStarted(IObjectWrapper session, String sessionId) = 3;
    void onSessionResumed(IObjectWrapper session, boolean wasSuspended) = 4;
    void onSessionEnding(IObjectWrapper session) = 5;
    void onSessionEnded(IObjectWrapper session, int error) = 6;
    void onSessionResuming(IObjectWrapper session, String sessionId) = 7;
    void onSessionResumeFailed(IObjectWrapper session, int error) = 8;
    void onSessionSuspended(IObjectWrapper session, int reason) = 9;
    int getSupportedVersion() = 10;
}
