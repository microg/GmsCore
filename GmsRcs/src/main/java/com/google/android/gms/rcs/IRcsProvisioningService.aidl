// IRcsProvisioningService.aidl
package com.google.android.gms.rcs;

import com.google.android.gms.rcs.IRcsProvisioningCallback;

interface IRcsProvisioningService {
    void startProvisioning(String carrierId, String msisdn, IRcsProvisioningCallback callback);
}
