package com.google.android.gms.auth.api.phone;

import android.os.Bundle;
import android.os.RemoteException;
import com.google.android.gms.auth.api.phone.IConstellationService;

public class ConstellationService extends IConstellationService.Stub {

    @Override
    public void verifyPhoneNumber(String phoneNumber, Bundle options) throws RemoteException {
        // We will eventually broadcast a success intent back to Messages here
        // For now, intercept the request and prevent the indefinite hang
    }

    @Override
    public boolean isVerificationPending() throws RemoteException {
        // Force Messages to assume the verification is complete
        return false;
    }

    @Override
    public Bundle getVerifiedPhoneNumber(Bundle requestBundle) throws RemoteException {
        Bundle response = new Bundle();
        // Provide a mocked positive attestation state
        response.putBoolean("is_verified", true);
        response.putString("verification_status", "SUCCESS");
        return response;
    }
}