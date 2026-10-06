/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.rcs.internal;

import com.google.android.gms.common.api.Status;

/**
 * Callbacks from IRcsService back to Google Messages (com.google.android.apps.messaging).
 *
 * Transaction IDs are matched to those observed in Play Services via reverse engineering.
 */
interface IRcsServiceCallbacks {
    // Called in response to getCapabilities / initialization
    void onStatus(in Status status) = 1;
    // Called with byte[] payload (proto-encoded RCS config / capability response)
    void onResult(in Status status, in byte[] data) = 2;
}
