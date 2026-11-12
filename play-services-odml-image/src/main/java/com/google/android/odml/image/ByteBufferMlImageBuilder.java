/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.odml.image;

import androidx.annotation.NonNull;

import org.microg.gms.common.PublicApi;

import java.nio.ByteBuffer;
import java.util.Objects;

@PublicApi
public class ByteBufferMlImageBuilder {
    private final ByteBuffer buffer;
    private final int width;
    private final int height;
    private final int imageFormat;
    private int rotation;

    public ByteBufferMlImageBuilder(@NonNull ByteBuffer byteBuffer, int width, int height,
                                    @MlImage.ImageFormat int imageFormat) {
        Objects.requireNonNull(byteBuffer, "ByteBuffer must not be null");
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Image dimensions must be positive");
        long pixels = (long) width * height;
        if (pixels > Integer.MAX_VALUE) throw new IllegalArgumentException("Image is too large for a ByteBuffer");
        long required;
        switch (imageFormat) {
            case MlImage.IMAGE_FORMAT_RGBA:
                required = pixels * 4;
                break;
            case MlImage.IMAGE_FORMAT_RGB:
                required = pixels * 3;
                break;
            case MlImage.IMAGE_FORMAT_ALPHA:
                required = pixels;
                break;
            case MlImage.IMAGE_FORMAT_NV12:
            case MlImage.IMAGE_FORMAT_NV21:
            case MlImage.IMAGE_FORMAT_YV12:
            case MlImage.IMAGE_FORMAT_YV21:
                if ((width & 1) != 0 || (height & 1) != 0) {
                    throw new IllegalArgumentException("YUV image dimensions must be even");
                }
                required = pixels + pixels / 2;
                break;
            default:
                throw new IllegalArgumentException("Unsupported ByteBuffer image format");
        }
        if (required > byteBuffer.limit()) throw new IllegalArgumentException("Image buffer is truncated");
        this.buffer = byteBuffer.asReadOnlyBuffer().order(byteBuffer.order());
        this.width = width;
        this.height = height;
        this.imageFormat = imageFormat;
    }

    @NonNull
    public ByteBufferMlImageBuilder setRotation(int rotation) {
        MlImage.checkRotation(rotation);
        this.rotation = rotation;
        return this;
    }

    @NonNull
    public MlImage build() {
        return new MlImage(buffer.asReadOnlyBuffer().order(buffer.order()), width, height, rotation,
                imageFormat, MlImage.STORAGE_TYPE_BYTEBUFFER);
    }
}
