package com.google.android.gms.cast.framework;

import com.google.android.gms.dynamic.IObjectWrapper;

interface ISessionProvider {
    IObjectWrapper getWrappedSession(String id) = 0;
    boolean isSessionRecoverable() = 1;
    String getCategory() = 2;
    int getSupportedVersion() = 3;
}
