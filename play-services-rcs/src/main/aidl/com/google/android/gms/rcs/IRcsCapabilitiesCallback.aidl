/*
 * SPDX-FileCopyrightText: 2024-2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.rcs;

import com.google.android.gms.rcs.RcsCapabilities;

/**
 * Callback for RCS capabilities queries
 */
interface IRcsCapabilitiesCallback {
    /**
     * Called when capabilities are successfully retrieved
     * @param phoneNumber The phone number queried
     * @param capabilities The capabilities object
     */
    void onCapabilitiesReceived(String phoneNumber, in RcsCapabilities capabilities);
    
    /**
     * Called when bulk capabilities are retrieved
     * @param results Map of phone numbers to capabilities
     */
    void onBulkCapabilitiesReceived(in Map results);
    
    /**
     * Called when capabilities query fails
     * @param phoneNumber The phone number that failed
     * @param errorCode Error code
     * @param errorMessage Human-readable error message
     */
    void onError(String phoneNumber, int errorCode, String errorMessage);
}
