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
@IntDef({PatchFormat.UNKNOWN_PATCHING_FORMAT, PatchFormat.PATCH_GDIFF, PatchFormat.GZIPPED_GDIFF, PatchFormat.GZIPPED_BSDIFF, PatchFormat.GZIPPED_FILEBYFILE, PatchFormat.BROTLI_FILEBYFILE, PatchFormat.BROTLI_BSDIFF, PatchFormat.BROTLI_FILEBYFILE_RECURSIVE, PatchFormat.BROTLI_FILEBYFILE_ANDROID_AWARE, PatchFormat.BROTLI_FILEBYFILE_RECURSIVE_ANDROID_AWARE, PatchFormat.BROTLI_FILEBYFILE_ANDROID_AWARE_NO_RECOMPRESSION})
public @interface PatchFormat {
    int UNKNOWN_PATCHING_FORMAT = 0;
    int PATCH_GDIFF = 1;
    int GZIPPED_GDIFF = 2;
    int GZIPPED_BSDIFF = 3;
    int GZIPPED_FILEBYFILE = 4;
    int BROTLI_FILEBYFILE = 5;
    int BROTLI_BSDIFF = 6;
    int BROTLI_FILEBYFILE_RECURSIVE = 7;
    int BROTLI_FILEBYFILE_ANDROID_AWARE = 8;
    int BROTLI_FILEBYFILE_RECURSIVE_ANDROID_AWARE = 9;
    int BROTLI_FILEBYFILE_ANDROID_AWARE_NO_RECOMPRESSION = 10;
}
