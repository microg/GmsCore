package com.google.android.gms.auth.api.phone;

import android.os.Bundle;
import android.os.RemoteException;
import com.google.android.gms.auth.api.phone.IAsterismService;

public class AsterismService extends IAsterismService.Stub {

    @Override
    public void checkConsentStatus(String phoneNumber, Bundle options) throws RemoteException {
        // Intercept consent request
    }

    @Override
    public boolean isProvisioningRequired() throws RemoteException {
        // Tell Messages that no further provisioning is required
        return false;
    }

    @Override
    public Bundle getConsentToken(Bundle requestBundle) throws RemoteException {
        Bundle response = new Bundle();
        // Provide a mocked positive consent token
        response.putBoolean("consent_granted", true);
        response.putString("consent_status", "SUCCESS");
        return response;
    }
}