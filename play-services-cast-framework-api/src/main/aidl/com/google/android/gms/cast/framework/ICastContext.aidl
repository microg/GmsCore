package com.google.android.gms.cast.framework;

import com.google.android.gms.cast.framework.ISessionManager;
import com.google.android.gms.cast.framework.IDiscoveryManager;
import com.google.android.gms.dynamic.IObjectWrapper;

interface ICastContext {
    Bundle getMergedSelectorAsBundle() = 0;
    boolean isApplicationVisible() = 1;
    //void removeAppVisibilityListener(IAppVisibilityListener listener) = 2;
    //void addAppVisibilityListener(IAppVisibilityListener listener) = 3;
    ISessionManager getSessionManagerImpl() = 4;
    IDiscoveryManager getDiscoveryManagerImpl() = 5;

    void destroy() = 6;
    void onActivityResumed(in IObjectWrapper activity) = 7;
    void onActivityPaused(in IObjectWrapper activity) = 8;
    IObjectWrapper getWrappedThis() = 9;
    void unknown(String s1, in Map m1) = 10; // TODO
}
