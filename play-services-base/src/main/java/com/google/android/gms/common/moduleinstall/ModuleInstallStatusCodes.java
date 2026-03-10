/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.common.moduleinstall;

import androidx.annotation.NonNull;
import com.google.android.gms.common.api.CommonStatusCodes;

/**
 * Status code for module install APIs.
 */
public class ModuleInstallStatusCodes extends CommonStatusCodes {
    /**
     * Status code indicating no error (success).
     */
    public static final int SUCCESS = 0;
    /**
     * Status code indicating the requested module is not recognized, and will not be available to download. A retry will not
     * resolve this error, but updating Google Play services may help.
     */
    public static final int UNKNOWN_MODULE = 46000;
    /**
     * Status code indicating the requested module is not allowed to be installed on this device.
     */
    public static final int NOT_ALLOWED_MODULE = 46001;
    /**
     * Status code indicating the requested module is not found.
     */
    public static final int MODULE_NOT_FOUND = 46002;
    /**
     * Status code indicating there is not enough disk space to install the requested module.
     */
    public static final int INSUFFICIENT_STORAGE = 46003;

    @NonNull
    public static String getStatusCodeString(int statusCode) {
        switch (statusCode) {
            case UNKNOWN_MODULE /* 46000 */:
                return "UNKNOWN_MODULE";
            case NOT_ALLOWED_MODULE /* 46001 */:
                return "NOT_ALLOWED_MODULE";
            case MODULE_NOT_FOUND /* 46002 */:
                return "MODULE_NOT_FOUND";
            case INSUFFICIENT_STORAGE /* 46003 */:
                return "INSUFFICIENT_STORAGE";
            default:
                return CommonStatusCodes.getStatusCodeString(statusCode);
        }
    }
}
