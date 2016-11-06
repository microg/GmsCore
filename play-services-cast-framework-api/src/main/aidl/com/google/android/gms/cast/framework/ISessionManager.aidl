package com.google.android.gms.cast.framework;

import com.google.android.gms.dynamic.IObjectWrapper;

interface ISessionManager {
    IObjectWrapper getWrappedCurrentSession() = 0;
    //void addSessionManagerListener(ISessionManagerListener listener) = 1;
    //void removeSessionManagerListener(ISessionManagerListener listener) = 2;
    //void addCastStateListener(ICastStateListener listener) = 3;
    //void removeCastStateListener(ICastStateListener listener) = 4;
    void endCurrentSession(boolean b, boolean stopCasting) = 5;
    IObjectWrapper getWrappedThis() = 6;
}