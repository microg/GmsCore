package com.google.android.gms.maps.model.internal;

interface IIndoorBuildingDelegate {
    int getActiveLevelIndex() = 0;
    int getDefaultLevelIndex() = 1;
    List<IBinder> getLevels() = 2; // IIndoorLevelDelegate's
    boolean isUnderground() = 3;
    boolean equalsRemote(IIndoorBuildingDelegate other) = 4;
    int hashCodeRemote() = 5;
}
