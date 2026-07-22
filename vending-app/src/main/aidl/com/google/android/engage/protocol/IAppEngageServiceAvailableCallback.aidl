/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.engage.protocol;

import android.os.Bundle;

/**
 * Callback interface for isServiceAvailable operation.
 * This callback is used to receive the availability status of the App Engage Service.
 */
interface IAppEngageServiceAvailableCallback {
    /**
     * Called with the service availability result.
     *
     * @param result Bundle containing availability information.
     *               The key "availability" contains a boolean indicating whether the service is available.
     */
    void onResult(in Bundle result);
}