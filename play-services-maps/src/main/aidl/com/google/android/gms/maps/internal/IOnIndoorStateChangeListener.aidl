package com.google.android.gms.maps.internal;

interface IOnIndoorStateChangeListener {
    void onIndoorBuildingFocused() = 0;
    void onIndoorLevelActivated() = 1;
}
