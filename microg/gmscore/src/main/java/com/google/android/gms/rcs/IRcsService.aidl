// AIDL interface for RCS service
package com.google.android.gms.rcs;

interface IRcsService {
    int getProvisioningState();
    boolean isRcsCapable();
    void startProvisioning(String phoneNumber);
    Map getRcsConfig();
}
