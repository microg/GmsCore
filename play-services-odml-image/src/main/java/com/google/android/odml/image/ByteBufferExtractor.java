/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.odml.image;

import java.nio.ByteBuffer;

import androidx.annotation.NonNull;

import org.microg.gms.common.PublicApi;

@PublicApi
public class ByteBufferExtractor {
    private ByteBufferExtractor() {
    }

    @NonNull
    public static ByteBuffer extract(@NonNull MlImage image) {
        ByteBuffer buffer = (ByteBuffer) image.getStorage(MlImage.STORAGE_TYPE_BYTEBUFFER);
        return buffer.asReadOnlyBuffer().order(buffer.order());
    }
}
