package com.google.android.gms.cast.framework;

import com.google.android.gms.cast.framework.ISessionManager;
import com.google.android.gms.dynamic.IObjectWrapper;

interface ICastContext {
    Bundle getMergedSelectorAsBundle() = 0;
    boolean isApplicationVisible() = 1;
    //void addAppVisibilityListener(IAppVisibilityListener listener) = 2;
    //void removeAppVisibilityListener(IAppVisibilityListener listener) = 3;
    ISessionManager getSessionManager() = 4;
    void destroy() = 5;
    void onActivityResumed(in IObjectWrapper activity) = 6;
    void onActivityPaused(in IObjectWrapper activity) = 7;
}