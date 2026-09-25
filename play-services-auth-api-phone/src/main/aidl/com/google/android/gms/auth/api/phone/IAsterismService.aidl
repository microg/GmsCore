package com.google.android.gms.auth.api.phone;

interface IAsterismService {
    // Defines the consent check Google Messages runs after verifying the number
    void checkConsentStatus(String phoneNumber, in Bundle options);

    // Fallback provisioning state
    boolean isProvisioningRequired();

    // Retrieves the successful consent token
    Bundle getConsentToken(in Bundle requestBundle);
}