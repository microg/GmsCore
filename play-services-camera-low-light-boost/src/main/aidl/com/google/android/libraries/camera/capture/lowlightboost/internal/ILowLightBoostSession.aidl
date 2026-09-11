/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.libraries.camera.capture.lowlightboost.internal;

import com.google.android.libraries.camera.capture.lowlightboost.internal.CaptureResultParcelable;

interface ILowLightBoostSession {
    oneway void processCaptureResult(in CaptureResultParcelable captureResult) = 0;
    int isLowLightBoostEnabled() = 1;
    oneway void enableLowLightBoost(int boostMode) = 2;
    oneway void release() = 3;
}
