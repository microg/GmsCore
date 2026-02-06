/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.libraries.entitlement.utils;

import static java.nio.charset.StandardCharsets.UTF_8;

import androidx.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Utility methods about {@link InputStream}. */
public final class StreamUtils {
    private static final int BUFFER_SIZE = 1024;

    private StreamUtils() {}

    /** Reads an {@link InputStream} into a UTF-8 string. */
    public static String inputStreamToString(@Nullable InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IOException("inputStream is null");
        }
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
             ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int length = 0;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString(UTF_8.name());
        }
    }

    /** Reads an {@link InputStream} into a UTF-8 string. Returns an empty string if any error. */
    public static String inputStreamToStringSafe(@Nullable InputStream inputStream) {
        try {
            return inputStreamToString(inputStream);
        } catch (IOException e) {
            return "";
        }
    }
}
