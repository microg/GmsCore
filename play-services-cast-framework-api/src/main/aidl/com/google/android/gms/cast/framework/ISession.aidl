package com.google.android.gms.cast.framework;

import com.google.android.gms.dynamic.IObjectWrapper;

interface ISession {
    IObjectWrapper getWrappedObject() = 0;
    String getCategory() = 1;
    String getSessionId() = 2;
    String getRouteId() = 3;
    boolean isConnected() = 4;
    boolean isConnecting() = 5;
    boolean isDisconnecting() = 6;
    boolean isDisconnected() = 7;
    boolean isResuming() = 8;
    boolean isSuspended() = 9;
    void notifySessionStarted(String sessionId) = 10;
    void notifyFailedToStartSession(int error) = 11;
    void notifySessionEnded(int error) = 12;
    void notifySessionResumed(boolean wasSuspended) = 13;
    void notifyFailedToResumeSession(int error) = 14;
    void notifySessionSuspended(int reason) = 15;
}
