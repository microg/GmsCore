package com.google.android.gms.cast.framework;

import com.google.android.gms.cast.framework.IAppVisibilityListener;
import com.google.android.gms.cast.framework.ISessionManager;
import com.google.android.gms.cast.framework.IDiscoveryManager;
import com.google.android.gms.dynamic.IObjectWrapper;

interface ICastContext {
    Bundle getMergedSelectorAsBundle() = 0;
    boolean isApplicationVisible() = 1;
    void addVisibilityChangeListener(IAppVisibilityListener listener) = 2;
    void removeVisibilityChangeListener(IAppVisibilityListener listener) = 3;
    ISessionManager getSessionManagerImpl() = 4;
    IDiscoveryManager getDiscoveryManagerImpl() = 5;
    void destroy() = 6; // deprecated?
    void onActivityResumed(in IObjectWrapper activity) = 7; // deprecated?
    void onActivityPaused(in IObjectWrapper activity) = 8; // deprecated?
    IObjectWrapper getWrappedThis() = 9;
    void setReceiverApplicationId(String receiverApplicationId, in Map/*<String, IBinder>*/ sessionProvidersByCategory) = 10;
}
