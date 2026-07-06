package com.google.android.gms.rcs;

interface IRcsService {
    boolean isRcsEnabled();
    void setRcsEnabled(boolean enabled);
    String getProvisioningToken();
    void requestProvisioning();
}
