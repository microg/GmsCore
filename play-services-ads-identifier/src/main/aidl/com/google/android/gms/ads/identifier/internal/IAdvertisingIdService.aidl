package com.google.android.gms.ads.identifier.internal;

import android.os.Bundle;

interface IAdvertisingIdService {
    String getAdvertisingId() = 0;
    boolean isAdTrackingLimited(boolean ignored) = 1;
    String resetAdvertisingId(String packageName) = 2;
    void setAdTrackingLimitedGlobally(String packageName, boolean limited) = 3;
    String setDebugLoggingEnabled(String packageName, boolean enabled) = 4;
    boolean isDebugLoggingEnabled() = 5;
    boolean isAdTrackingLimitedGlobally() = 6;
    void setAdTrackingLimitedForApp(int uid, boolean limited) = 7;
    void resetAdTrackingLimitedForApp(int uid) = 8;
    Bundle getAllAppsLimitedAdTrackingConfiguration() = 9; // Map packageName -> Boolean
    String getAdvertisingIdForApp(int uid) = 10;
}
