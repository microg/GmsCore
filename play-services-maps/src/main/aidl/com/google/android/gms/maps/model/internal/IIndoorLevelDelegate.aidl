package com.google.android.gms.maps.model.internal;

interface IIndoorLevelDelegate {
    String getName() = 0;
    String getShortName() = 1;
    void activate() = 2;
    boolean equalsRemote(IIndoorLevelDelegate other) = 3;
    int hashCodeRemote() = 4;
}
