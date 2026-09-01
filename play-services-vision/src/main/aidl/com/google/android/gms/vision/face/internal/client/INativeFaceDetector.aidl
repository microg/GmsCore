/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.vision.face.internal.client;

import com.google.android.gms.vision.face.internal.client.FaceParcel;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.vision.internal.FrameMetadataParcel;

interface INativeFaceDetector {
    FaceParcel[] detectFaceParcels(IObjectWrapper byteBuffer, in FrameMetadataParcel metadata) = 0;
    boolean isNativeFaceDetectorAvailable(int type) = 1;
    void closeDetectorJni() = 2;
    FaceParcel[] detectFacesFromPlanes(IObjectWrapper planeFirst, IObjectWrapper planeSecond, IObjectWrapper planeThird, int firstPixelStride, int secondPixelStride, int thirdPixelStride, int firstRowStride, int secondRowStride, int thirdRowStride, in FrameMetadataParcel metadata) = 3;
}