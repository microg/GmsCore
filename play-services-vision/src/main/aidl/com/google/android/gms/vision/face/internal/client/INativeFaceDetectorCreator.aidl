/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.vision.face.internal.client;

import com.google.android.gms.vision.face.internal.client.DetectionOptions;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.vision.face.internal.client.INativeFaceDetector;

interface INativeFaceDetectorCreator {
    INativeFaceDetector newFaceDetector(IObjectWrapper context, in DetectionOptions detectionOptions) = 0;
}