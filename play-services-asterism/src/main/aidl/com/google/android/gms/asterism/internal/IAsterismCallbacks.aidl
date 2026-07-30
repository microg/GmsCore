/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.asterism.internal;

/**
 * Callback interface for Asterism consent state changes in RCS pipeline.
 */
oneway interface IAsterismCallbacks {
    void onConsentStateChanged(int consentState, long timestamp) = 0;
    void onAsterismError(int errorCode, String errorMessage) = 1;
}
