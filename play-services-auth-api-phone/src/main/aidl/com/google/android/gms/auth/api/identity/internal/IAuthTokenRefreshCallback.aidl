package com.google.android.gms.auth.api.identity.internal;

import android.os.Bundle;

import com.google.android.gms.common.api.Status;

/**
 * Callback interface for authentication token refresh events.
 * Tokens need periodic renewal to maintain RCS session validity.
 */
interface IAuthTokenRefreshCallback {
    /**
     * Called when a new authentication token is generated.
     * 
     * @param status Status of token generation
     * @param tokenData Bundle containing:
     *   - "token": String (hex-encoded)
     *   - "expires_at_ms": Long
     *   - "refresh_token": String (for offline renewal)
     */
    void onTokenRefreshed(Status status, Bundle tokenData) = 0;
}

/**
 * Callback interface for RCS registration state changes.
 * Used by RCS messaging clients to track device provisioning status.
 */
interface IRegistrationStateCallback {
    /**
     * Called when registration status changes.
     * 
     * @param status Overall operation status
     * @param isRegistered Whether device is now registered with RCS
     * @param details Additional registration metadata
     */
    void onRegistrationStateChanged(Status status, boolean isRegistered, Bundle details) = 1;
}
