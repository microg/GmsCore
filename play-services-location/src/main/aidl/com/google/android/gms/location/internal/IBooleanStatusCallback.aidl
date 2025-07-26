/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.location.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationSettingsResult;

interface IBooleanStatusCallback {
    void statusCallback(in Status status, in boolean result);
}
