package com.google.android.gms.ads.identifier.internal;

interface IAdvertisingIdService {
    String getAdvertisingId() = 0;
    boolean isAdTrackingLimited(boolean defaultHint) = 1;
    String generateAdvertisingId(String packageName) = 2;
    void setAdTrackingLimited(String packageName, boolean limited) = 3;
}
