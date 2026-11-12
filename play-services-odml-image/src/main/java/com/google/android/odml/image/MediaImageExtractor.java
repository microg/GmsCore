/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.odml.image;

import android.media.Image;

import androidx.annotation.NonNull;

import org.microg.gms.common.PublicApi;

@PublicApi
public class MediaImageExtractor {
    private MediaImageExtractor() {
    }

    @NonNull
    public static Image extract(@NonNull MlImage image) {
        return (Image) image.getStorage(MlImage.STORAGE_TYPE_MEDIA_IMAGE);
    }
}
