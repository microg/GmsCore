/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.libraries.camera.capture.lowlightboost.internal;

import com.google.android.libraries.camera.capture.lowlightboost.internal.ILowLightBoostCallback;
import com.google.android.libraries.camera.capture.lowlightboost.internal.LowLightBoostOptionsParcelable;

interface ILowLightBoostService {
    oneway void createSession(in LowLightBoostOptionsParcelable options, ILowLightBoostCallback callback) = 0;
}
