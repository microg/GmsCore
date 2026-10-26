/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.libraries.camera.capture.lowlightboost.internal;

import android.view.Surface;
import com.google.android.libraries.camera.capture.lowlightboost.internal.ILowLightBoostSession;

oneway interface ILowLightBoostCallback {
    void onSessionCreated(int statusCode, ILowLightBoostSession session, in Surface cameraSurface) = 0;
    void onSessionDisconnected(int statusCode) = 1;
    void onSceneBrightnessChanged(float brightness) = 2;
    void onSessionDestroyed() = 3;
}
