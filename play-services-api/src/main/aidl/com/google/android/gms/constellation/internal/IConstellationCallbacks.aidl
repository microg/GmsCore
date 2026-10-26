/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.constellation.VerificationResult;

interface IConstellationCallbacks {
    void onVerificationResult(in Status status, in VerificationResult result);
}
