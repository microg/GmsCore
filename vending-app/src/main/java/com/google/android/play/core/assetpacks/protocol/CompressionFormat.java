/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.play.core.assetpacks.protocol;

import androidx.annotation.IntDef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE_USE})
@Retention(RetentionPolicy.CLASS)
@IntDef({CompressionFormat.UNSPECIFIED, CompressionFormat.BROTLI, CompressionFormat.GZIP, CompressionFormat.CHUNKED_GZIP, CompressionFormat.CHUNKED_BROTLI})
public @interface CompressionFormat {
    int UNSPECIFIED = 0;
    int BROTLI = 1;
    int GZIP = 2;
    int CHUNKED_GZIP = 3;
    int CHUNKED_BROTLI = 4;
}
