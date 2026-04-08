package com.google.android.gms.auth.api.identity.internal;

import android.os.Bundle;

import com.google.android.gms.common.api.Status;

/**
 * Callback interface for network configuration events.
 * Used to notify clients when carrier configuration changes are detected.
 */
interface INetworkConfigCallback {
    /**
     * Called when a new network configuration is available.
     * 
     * @param status Status of the configuration update
     * @param configData Raw configuration bundle from carrier
     */
    void onNetworkConfigAvailable(Status status, Bundle configData) = 0;
    
    /**
     * Called when network connectivity to constellation server is lost.
     */
    void onNetworkUnavailable(Status status) = 1;
}
