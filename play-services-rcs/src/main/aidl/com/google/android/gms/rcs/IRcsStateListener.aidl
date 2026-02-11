/*
 * SPDX-FileCopyrightText: 2024-2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.rcs;

/**
 * Listener for RCS state changes
 */
interface IRcsStateListener {
    
    // RCS States
    const int STATE_DISABLED = 0;
    const int STATE_CONNECTING = 1;
    const int STATE_CONNECTED = 2;
    const int STATE_DISCONNECTED = 3;
    const int STATE_ERROR = -1;
    
    /**
     * Called when RCS state changes
     * @param state New RCS state
     */
    void onRcsStateChanged(int state);
    
    /**
     * Called when RCS registration state changes
     * @param registered Whether RCS is registered
     * @param phoneNumber The registered phone number (null if not registered)
     */
    void onRegistrationStateChanged(boolean registered, String phoneNumber);
    
    /**
     * Called when there's an RCS error
     * @param errorCode Error code
     * @param errorMessage Error message
     */
    void onError(int errorCode, String errorMessage);
}
