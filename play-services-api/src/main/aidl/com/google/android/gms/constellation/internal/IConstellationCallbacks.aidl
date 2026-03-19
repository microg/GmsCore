/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation.internal;

import com.google.android.gms.common.api.Status;

interface IConstellationCallbacks {
    void onPhoneNumber(in Status status, String phoneNumber) = 0;
    void onVerificationResult(in Status status, boolean success) = 1;
}
