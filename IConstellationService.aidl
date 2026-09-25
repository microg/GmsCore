package com.google.android.gms.auth.api.phone;

interface IConstellationService {
    // This is the primary verification endpoint Google Messages hits during "Setting up..."
    void verifyPhoneNumber(String phoneNumber, in Bundle options);

    // Fallback status check
    boolean isVerificationPending();

    // Retrieves the successfully verified number state
    Bundle getVerifiedPhoneNumber(in Bundle requestBundle);
}