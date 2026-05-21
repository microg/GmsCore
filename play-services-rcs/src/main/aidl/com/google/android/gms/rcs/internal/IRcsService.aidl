package com.google.android.gms.rcs.internal;

interface IRcsService {
    // Basic stub for RCS Provisioning and State
    boolean isRcsEnabled() = 0;
    void setRcsEnabled(boolean enabled) = 1;
    int getRcsState() = 2;
    String getProvisioningUrl() = 3;
    void notifyProvisioningSuccess(String token) = 4;
}
