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

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

public class BytesConverter {
    private static final int INTEGER_SIZE = 4; // 4 bytes

    // A table mapping from a number to a hex character for fast encoding hex strings.
    private static final char[] HEX_CHARS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    /**
     * Converts a byte array into a String of hexadecimal characters.
     *
     * @param bytes an array of bytes
     * @return hex string representation of bytes array
     */
    @Nullable
    public static String convertBytesToHexString(byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        StringBuilder ret = new StringBuilder(2 * bytes.length);

        for (int i = 0; i < bytes.length; i++) {
            int b;
            b = 0x0f & (bytes[i] >> 4);
            ret.append(HEX_CHARS[b]);
            b = 0x0f & bytes[i];
            ret.append(HEX_CHARS[b]);
        }

        return ret.toString();
    }

    /**
     * Converts integer to 4 bytes.
     */
    public static byte[] convertIntegerTo4Bytes(int value) {
        return ByteBuffer.allocate(INTEGER_SIZE).putInt(value).array();
    }
}
