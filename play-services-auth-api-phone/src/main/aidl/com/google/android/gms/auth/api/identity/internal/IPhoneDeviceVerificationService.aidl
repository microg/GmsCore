package com.google.android.gms.auth.api.identity.internal;

import android.os.Bundle;

import com.google.android.gms.common.api.Status;

/**
 * Callback interface for device verification initialization result.
 * Used by IPhoneDeviceVerificationService to notify clients when the service is ready.
 */
interface IDeviceVerificationCallback {
    void onDeviceVerificationInitialized(Status status) = 0;
}

/**
 * AIDL interface for Constellation API - Phone Device Verification Service.
 * 
 * This interface enables RCS provisioning through Google's Constellation network,
 * which handles carrier configuration and authentication token generation.
 * 
 * Key functionality:
 * - Initialize device verification with IMSI/IMEI
 * - Generate authentication tokens for GSMA HTTP provisioning flow
 * - Handle carrier-specific configuration URLs (Jibe Cloud, 3GPP, etc.)
 * 
 * Reference: GmsCore Issue #2994 RCS Support ($10,000 BountyHub)
 */
interface IPhoneDeviceVerificationService {
    /**
     * Initialize the device verification process.
     * 
     * @param callback Callback to receive initialization status
     * @return Status code (0 = success, non-zero = error)
     */
    int initialize(IDeviceVerificationCallback callback) = 0;
    
    /**
     * Generate an authentication token for RCS provisioning.
     * The token is used in HTTP requests to carrier provisioning endpoints.
     * 
     * @param bundle Contains:
     *   - "imsi": String IMSI number
     *   - "imei": String IMEI number  
     *   - "random_bytes": byte[] for token entropy
     * @return Bundle containing:
     *   - "token": String authentication token (hex-encoded)
     *   - "expires_at_ms": Long timestamp in milliseconds
     *   - "carrier_config_url": String provisioning endpoint URL
     */
    Bundle generateAuthToken(Bundle bundle) = 1;
    
    /**
     * Fetch carrier configuration based on device capabilities.
     * Returns XML or JSON configuration describing supported RCS features.
     * 
     * @param bundle Contains device capability flags
     * @return Bundle with:
     *   - "config_data": String (XML/JSON config)
     *   - "config_format": String ("xml" or "json")
     *   - "carrier_name": String operator name
     */
    Bundle fetchCarrierConfig(Bundle bundle) = 2;
    
    /**
     * Verify if the current device is registered with RCS.
     * Checks against carrier databases via constellation network.
     * 
     * @param bundle Contains subscription info
     * @return Bundle with:
     *   - "is_registered": boolean
     *   - "registration_status": String ("active", "inactive", "not_found")
     *   - "last_seen_timestamp": Long
     */
    Bundle checkRegistrationStatus(Bundle bundle) = 3;
    
    /**
     * Submit a message delivery report to the constellation network.
     * Used for RCS messaging analytics and billing.
     * 
     * @param bundle Contains message metadata
     * @return Status of submission
     */
    int submitDeliveryReport(Bundle bundle) = 4;
}
