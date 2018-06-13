package com.google.android.gms.cast.framework;

import com.google.android.gms.dynamic.IObjectWrapper;

interface ISession {
    IObjectWrapper getWrappedObject() = 0;
    String getCategory() = 1;
    String getSessionId() = 2;
    boolean isConnected() = 4;
    boolean isConnecting() = 5;
    boolean isDisconnecting() = 6;
    boolean isDisconnected() = 7;
    boolean isResuming() = 8;
    boolean isSuspended() = 9;
    void notifySessionStarted(String s) = 10;
    void notifyFailedToStartSession(int e) = 11;
    void notifySessionEnded(int e) = 12;
    void notifySessionResumed(boolean b) = 13;
    void notifyFailedToResumeSession(int e) = 14;
    void notifySessionSuspended(int n) = 15;
}
