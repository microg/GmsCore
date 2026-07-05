package com.google.android.gms.rcs;

interface IRcsService {
    boolean isRcsAvailable();
    void setRcsEnabled(boolean enabled);
    void startRegistration();
    void stopRegistration();
    void getRegistrationStatus(IRcsStatusCallback callback);
}
