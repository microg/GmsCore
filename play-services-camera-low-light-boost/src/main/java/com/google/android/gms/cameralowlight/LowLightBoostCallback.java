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
 * Callbacks for a {@link LowLightBoostSession}.
 */
public interface LowLightBoostCallback {
    /**
     * Called when the {@link LowLightBoostSession} is destroyed.
     */
    void onSessionDestroyed();

    /**
     * Called when a {@link LowLightBoostSession} encounters an error.
     *
     * @param status the reason for the failure.
     */
    void onSessionDisconnected(@NonNull Status status);
}
