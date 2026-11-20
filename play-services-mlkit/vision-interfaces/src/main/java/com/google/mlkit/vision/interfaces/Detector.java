/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.interfaces;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.Image;

import androidx.lifecycle.LifecycleObserver;

import com.google.android.gms.tasks.Task;

import org.microg.gms.common.Hide;
import org.microg.gms.common.PublicApi;

import java.io.Closeable;
import java.nio.ByteBuffer;

@PublicApi
public interface Detector<DetectionResultT> extends Closeable, LifecycleObserver {
    @Hide
    int TYPE_TEXT_RECOGNITION = 4;

    @Hide
    int getDetectorType();

    @Hide
    Task<DetectionResultT> process(Bitmap bitmap, int rotationDegrees);

    @Hide
    Task<DetectionResultT> process(Image image, int rotationDegrees);

    @Hide
    Task<DetectionResultT> process(Image image, int rotationDegrees, Matrix coordinatesMatrix);

    @Hide
    Task<DetectionResultT> process(ByteBuffer buffer, int width, int height, int rotationDegrees, int format);

    @Override
    void close();
}
