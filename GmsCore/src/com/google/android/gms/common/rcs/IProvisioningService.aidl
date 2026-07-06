package com.google.android.gms.common.rcs;

interface IProvisioningService {
    void startProvisioning(String userAgent, String carrierId, IProvisioningCallback callback);
    void cancelProvisioning();
}