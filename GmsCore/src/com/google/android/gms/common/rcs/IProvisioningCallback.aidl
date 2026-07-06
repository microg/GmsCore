package com.google.android.gms.common.rcs;

interface IProvisioningCallback {
    void onProvisioningComplete(in Bundle result);
    void onError(int errorCode);
}