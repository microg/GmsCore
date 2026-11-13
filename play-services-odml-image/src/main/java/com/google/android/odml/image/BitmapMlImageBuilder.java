/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.odml.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import androidx.annotation.NonNull;

import org.microg.gms.common.PublicApi;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

@PublicApi
public class BitmapMlImageBuilder {
    private final Bitmap bitmap;
    private int rotation;

    public BitmapMlImageBuilder(@NonNull Bitmap bitmap) {
        this.bitmap = Objects.requireNonNull(bitmap, "Bitmap must not be null");
        if (bitmap.isRecycled()) throw new IllegalArgumentException("Bitmap is recycled");
    }

    public BitmapMlImageBuilder(@NonNull Context context, @NonNull Uri uri) throws IOException {
        Objects.requireNonNull(context, "Context must not be null");
        Objects.requireNonNull(uri, "Uri must not be null");
        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            if (stream == null) throw new IOException("Unable to open image");
            bitmap = BitmapFactory.decodeStream(stream);
            if (bitmap == null) throw new IOException("Unable to decode image");
        }
    }

    @NonNull
    public BitmapMlImageBuilder setRotation(int rotation) {
        MlImage.checkRotation(rotation);
        this.rotation = rotation;
        return this;
    }

    @NonNull
    public MlImage build() {
        if (bitmap.isRecycled()) throw new IllegalArgumentException("Bitmap is recycled");
        int format = MlImage.IMAGE_FORMAT_UNKNOWN;
        if (bitmap.getConfig() == Bitmap.Config.ARGB_8888) format = MlImage.IMAGE_FORMAT_RGBA;
        if (bitmap.getConfig() == Bitmap.Config.ALPHA_8) format = MlImage.IMAGE_FORMAT_ALPHA;
        return new MlImage(bitmap, bitmap.getWidth(), bitmap.getHeight(), rotation, format, MlImage.STORAGE_TYPE_BITMAP);
    }
}
