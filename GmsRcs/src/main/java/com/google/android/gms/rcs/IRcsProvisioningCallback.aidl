// IRcsProvisioningCallback.aidl
package com.google.android.gms.rcs;

interface IRcsProvisioningCallback {
    void onProvisioningSucceeded(boolean success);
    void onProvisioningFailed(int errorCode);
}
