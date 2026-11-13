/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.odml.image;

import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.media.Image;

import androidx.annotation.NonNull;

import org.microg.gms.common.PublicApi;

import java.util.Objects;

@PublicApi
public class MediaMlImageBuilder {
    private final Image image;
    private int rotation;

    public MediaMlImageBuilder(@NonNull Image mediaImage) {
        image = Objects.requireNonNull(mediaImage, "Image must not be null");
    }

    @NonNull
    public MediaMlImageBuilder setRotation(int rotation) {
        MlImage.checkRotation(rotation);
        this.rotation = rotation;
        return this;
    }

    @NonNull
    public MlImage build() {
        int format;
        switch (image.getFormat()) {
            case ImageFormat.YUV_420_888:
                format = MlImage.IMAGE_FORMAT_YUV_420_888;
                break;
            case ImageFormat.NV21:
                format = MlImage.IMAGE_FORMAT_NV21;
                break;
            case ImageFormat.YV12:
                format = MlImage.IMAGE_FORMAT_YV12;
                break;
            case ImageFormat.JPEG:
                format = MlImage.IMAGE_FORMAT_JPEG;
                break;
            case PixelFormat.RGBA_8888:
                format = MlImage.IMAGE_FORMAT_RGBA;
                break;
            case PixelFormat.RGB_888:
                format = MlImage.IMAGE_FORMAT_RGB;
                break;
            default:
                format = MlImage.IMAGE_FORMAT_UNKNOWN;
        }
        return new MlImage(image, image.getWidth(), image.getHeight(), rotation, format, MlImage.STORAGE_TYPE_MEDIA_IMAGE);
    }
}
