package com.google.android.gms.cast.framework;

import com.google.android.gms.dynamic.IObjectWrapper;

interface IDiscoveryManagerListener {
    IObjectWrapper getWrappedThis() = 0;
    void onDeviceAvailabilityChanged(boolean deviceAvailable) = 1;
}
