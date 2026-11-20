/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.odml.image;

import org.microg.gms.common.Hide;
import org.microg.gms.common.PublicApi;

@PublicApi
public abstract class ImageProperties {
    @Hide
    ImageProperties() {
    }

    @MlImage.ImageFormat
    public abstract int getImageFormat();

    @MlImage.StorageType
    public abstract int getStorageType();

    static ImageProperties create(int imageFormat, int storageType) {
        return new Properties(imageFormat, storageType);
    }

    private static final class Properties extends ImageProperties {
        private final int imageFormat;
        private final int storageType;

        private Properties(int imageFormat, int storageType) {
            this.imageFormat = imageFormat;
            this.storageType = storageType;
        }

        @Override
        public int getImageFormat() {
            return imageFormat;
        }

        @Override
        public int getStorageType() {
            return storageType;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof ImageProperties)) return false;
            ImageProperties properties = (ImageProperties) other;
            return imageFormat == properties.getImageFormat() && storageType == properties.getStorageType();
        }

        @Override
        public int hashCode() {
            return 31 * imageFormat + storageType;
        }

        @Override
        public String toString() {
            return "ImageProperties{imageFormat=" + imageFormat + ", storageType=" + storageType + "}";
        }
    }
}
