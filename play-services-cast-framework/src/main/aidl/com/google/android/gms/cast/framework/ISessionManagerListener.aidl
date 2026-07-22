package com.google.android.gms.cast.framework;

import com.google.android.gms.dynamic.IObjectWrapper;

interface ISessionManagerListener {
    IObjectWrapper getWrappedThis() = 0;
    void onSessionStarting(IObjectWrapper session) = 1;
    void onSessionStarted(IObjectWrapper session, String sessionId) = 2;
    void onSessionStartFailed(IObjectWrapper session, int error) = 3;
    void onSessionEnding(IObjectWrapper session) = 4;
    void onSessionEnded(IObjectWrapper session, int error) = 5;
    void onSessionResuming(IObjectWrapper session, String sessionId) = 6;
    void onSessionResumed(IObjectWrapper session, boolean wasSuspended) = 7;
    void onSessionResumeFailed(IObjectWrapper session, int error) = 8;
    void onSessionSuspended(IObjectWrapper session, int reason) = 9;
    int getSupportedVersion() = 10;
}
