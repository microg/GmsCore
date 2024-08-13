/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.util;

import android.database.CharArrayBuffer;
import android.graphics.Bitmap;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;

public final class DataUtils {

    public static void copyStringToBuffer(String desc, CharArrayBuffer dataOut) {
        if (TextUtils.isEmpty(desc)) {
            dataOut.sizeCopied = 0;
        } else if (dataOut.data != null && dataOut.data.length >= desc.length()) {
            desc.getChars(0, desc.length(), dataOut.data, 0);
        } else {
            dataOut.data = desc.toCharArray();
        }
        dataOut.sizeCopied = desc.length();
    }

    public static byte[] loadImageBytes(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }
}
