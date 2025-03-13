/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.face.aidls;

import com.google.android.gms.dynamic.IObjectWrapper;
import java.util.List;
import com.google.mlkit.vision.face.FrameMetadataParcel;
import com.google.mlkit.vision.face.Face;

interface IFaceDetector {
    void initDetector() = 0;
    void close() = 1;
    List<Face> detectFaces(IObjectWrapper wrapper, in FrameMetadataParcel metadata) = 2;
}