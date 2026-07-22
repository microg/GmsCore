package com.google.android.gms.cast.framework;

import com.google.android.gms.dynamic.IObjectWrapper;

interface IAppVisibilityListener {
    IObjectWrapper getThisObject() = 0;
    void onAppEnteredForeground() = 1;
    void onAppEnteredBackground() = 2;
    int getSupportedVersion() = 3;
}