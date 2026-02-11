/*
 * SPDX-FileCopyrightText: 2024-2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.rcs;

/**
 * Callback for RCS provisioning operations
 */
interface IRcsProvisioningCallback {
    
    // Provisioning status codes
    const int STATUS_NOT_PROVISIONED = 0;
    const int STATUS_PROVISIONING = 1;
    const int STATUS_PROVISIONED = 2;
    const int STATUS_ERROR = -1;
    
    // Error codes
    const int ERROR_NONE = 0;
    const int ERROR_NETWORK = 1;
    const int ERROR_CARRIER_NOT_SUPPORTED = 2;
    const int ERROR_SIM_NOT_READY = 3;
    const int ERROR_PERMISSION_DENIED = 4;
    const int ERROR_DEVICE_NOT_TRUSTED = 5;
    const int ERROR_TIMEOUT = 6;
    const int ERROR_UNKNOWN = 99;
    
    /**
     * Called when provisioning status changes
     * @param status Current provisioning status
     */
    void onProvisioningStatus(int status);
    
    /**
     * Called with provisioning progress
     * @param progress Progress percentage (0-100)
     * @param message Human-readable status message
     */
    void onProvisioningProgress(int progress, String message);
    
    /**
     * Called when provisioning completes successfully
     * @param phoneNumber The registered phone number
     */
    void onProvisioningComplete(String phoneNumber);
    
    /**
     * Called when provisioning fails
     * @param errorCode Error code
     * @param errorMessage Human-readable error message
     */
    void onProvisioningError(int errorCode, String errorMessage);
}
