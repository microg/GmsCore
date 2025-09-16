/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.face.aidls;

import com.google.mlkit.vision.face.aidls.IFaceDetector;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.mlkit.vision.face.FaceDetectionOptions;

interface IFaceDetectorCreator {
    IFaceDetector newFaceDetector(IObjectWrapper context, in FaceDetectionOptions faceDetectionOptions) = 0;
}