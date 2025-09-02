/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.rcs.internal;

import com.google.android.gms.common.api.Status;
import android.os.Bundle;

interface IRcsCallbacks {
    void onCapabilities(in Status status, in Bundle capabilities) = 0;
    void onAvailability(in Status status, boolean available) = 1;
    void onConfiguration(in Status status, in Bundle config) = 2;
    void onProvisioningStatus(in Status status, int provisioningStatus) = 3;
    void onResult(in Status status) = 4;
}
