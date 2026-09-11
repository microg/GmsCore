/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.cameralowlight;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.Status;

import org.microg.gms.common.PublicApi;

/**
 * Receives low light boost session lifecycle events.
 */
@PublicApi
public interface LowLightBoostCallback {
    /**
     * Called when the session is closed and its resources have been released.
     */
    void onSessionDestroyed();

    /**
     * Called when the session is disconnected because of an error.
     */
    void onSessionDisconnected(@NonNull Status status);
}
