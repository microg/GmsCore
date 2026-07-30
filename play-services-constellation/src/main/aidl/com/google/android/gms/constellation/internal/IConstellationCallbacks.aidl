/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation.internal;

/**
 * Callback interface for Constellation verification events.
 */
oneway interface IConstellationCallbacks {
    void onVerificationComplete(int status, String phoneNumber, long timestamp) = 0;
    void onPnvCapabilitiesUpdated(int capabilities, long timestamp) = 1;
    void onConstellationError(int errorCode, String errorMessage) = 2;
    void onIidTokenRefreshed(String token, long expiryTimestamp) = 3;
}
