package com.google.android.gms.rcs;

import com.google.android.gms.rcs.IRcsProvisioningCallback;

interface IRcsProvisioningService {
    void getRcsProvisioningStatus(IRcsProvisioningCallback callback);
    void updateRcsProvisioningStatus(int status, IRcsProvisioningCallback callback);
}
