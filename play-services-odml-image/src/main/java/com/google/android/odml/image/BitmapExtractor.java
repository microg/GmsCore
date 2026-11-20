/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.odml.image;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import org.microg.gms.common.PublicApi;

@PublicApi
public final class BitmapExtractor {
    private BitmapExtractor() {
    }

    @NonNull
    public static Bitmap extract(@NonNull MlImage image) {
        return (Bitmap) image.getStorage(MlImage.STORAGE_TYPE_BITMAP);
    }
}
