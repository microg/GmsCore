/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.odml.image;

import android.graphics.Bitmap;
import android.media.Image;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import org.microg.gms.common.Hide;
import org.microg.gms.common.PublicApi;

import java.io.Closeable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@PublicApi
public class MlImage implements Closeable {
    public static final int IMAGE_FORMAT_UNKNOWN = 0;
    public static final int IMAGE_FORMAT_RGBA = 1;
    public static final int IMAGE_FORMAT_RGB = 2;
    public static final int IMAGE_FORMAT_NV12 = 3;
    public static final int IMAGE_FORMAT_NV21 = 4;
    public static final int IMAGE_FORMAT_YV12 = 5;
    public static final int IMAGE_FORMAT_YV21 = 6;
    public static final int IMAGE_FORMAT_YUV_420_888 = 7;
    public static final int IMAGE_FORMAT_ALPHA = 8;
    public static final int IMAGE_FORMAT_JPEG = 9;

    public static final int STORAGE_TYPE_BITMAP = 1;
    public static final int STORAGE_TYPE_BYTEBUFFER = 2;
    public static final int STORAGE_TYPE_MEDIA_IMAGE = 3;

    private final int width;
    private final int height;
    private final int rotation;
    private final List<ImageProperties> properties;
    private final Internal internal = new Internal(this);
    private Object image;
    private int referenceCount = 1;

    @Hide
    MlImage(Object image, int width, int height, int rotation, int format, int storageType) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Image dimensions must be positive");
        checkRotation(rotation);
        this.image = Objects.requireNonNull(image, "Image must not be null");
        this.width = width;
        this.height = height;
        this.rotation = rotation;
        this.properties = Collections.singletonList(ImageProperties.create(format, storageType));
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getRotation() {
        return rotation;
    }

    @NonNull
    public List<ImageProperties> getContainedImageProperties() {
        return properties;
    }

    @NonNull
    public Internal getInternal() {
        return internal;
    }

    /**
     * Releases one reference; the last reference releases storage supplied to the builder.
     */
    @Override
    public synchronized void close() {
        if (referenceCount == 0) return;
        if (--referenceCount != 0) return;
        Object storage = image;
        image = null;
        if (storage instanceof Bitmap) {
            ((Bitmap) storage).recycle();
        } else if (storage instanceof Image) {
            ((Image) storage).close();
        }
    }

    synchronized Object getStorage(@StorageType int expectedType) {
        requireOpen();
        if (properties.get(0).getStorageType() != expectedType) {
            throw new IllegalArgumentException("Image does not contain the requested storage type");
        }
        return image;
    }

    private void requireOpen() {
        if (referenceCount == 0) throw new IllegalStateException("MlImage is closed");
    }

    static void checkRotation(int rotation) {
        if (rotation != 0 && rotation != 90 && rotation != 180 && rotation != 270) {
            throw new IllegalArgumentException("Rotation must be 0, 90, 180 or 270");
        }
    }

    @PublicApi
    public static final class Internal {
        private final MlImage owner;

        private Internal(MlImage owner) {
            this.owner = owner;
        }

        public void acquire() {
            synchronized (owner) {
                owner.requireOpen();
                if (owner.referenceCount == Integer.MAX_VALUE) throw new IllegalStateException("Too many image references");
                owner.referenceCount++;
            }
        }
    }

    @PublicApi
    @IntDef({IMAGE_FORMAT_UNKNOWN, IMAGE_FORMAT_RGBA, IMAGE_FORMAT_RGB, IMAGE_FORMAT_NV12,
            IMAGE_FORMAT_NV21, IMAGE_FORMAT_YV12, IMAGE_FORMAT_YV21, IMAGE_FORMAT_YUV_420_888,
            IMAGE_FORMAT_ALPHA, IMAGE_FORMAT_JPEG})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ImageFormat {
    }

    @PublicApi
    @IntDef({STORAGE_TYPE_BITMAP, STORAGE_TYPE_BYTEBUFFER, STORAGE_TYPE_MEDIA_IMAGE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface StorageType {
    }
}
