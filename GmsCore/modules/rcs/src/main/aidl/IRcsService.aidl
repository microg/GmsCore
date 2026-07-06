package org.microg.gms.rcs;

interface IRcsService {
    boolean isRcsAvailable();
    void startProvisioning();
    void verifyPhoneNumber(String phoneNumber);
}
