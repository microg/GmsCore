/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard;

import android.util.Base64;

public class Utils {
    public static byte[] getErrorBytes(String s) {
        return ("ERROR : " + s).getBytes();
    }

    public static String toBase64(byte[] result) {
        return Base64.encodeToString(result, Base64.URL_SAFE + Base64.NO_WRAP + Base64.NO_PADDING);
    }
}
