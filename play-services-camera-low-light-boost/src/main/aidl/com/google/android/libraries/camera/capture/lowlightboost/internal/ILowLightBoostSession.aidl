/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.libraries.camera.capture.lowlightboost.internal;

import com.google.android.libraries.camera.capture.lowlightboost.internal.FrameMetadata;

interface ILowLightBoostSession {
    oneway void processCaptureResult(in FrameMetadata frameMetadata) = 0;
    int getLowLightBoostMode() = 1;
    oneway void setLowLightBoostMode(int boostMode) = 2;
    oneway void release() = 3;
}
