/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.accountsettings.ui.bridge

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION.RELEASE
import android.util.Log
import android.webkit.JavascriptInterface
import org.microg.gms.common.Constants

class OcClientInfoBridge() {

    companion object {
        const val NAME = "ocClientInfo"
        private const val TAG = "JS_$NAME"
    }

    @JavascriptInterface
    fun getGmsCoreModuleApkVersionName(): String? {
        Log.d(TAG, "getGmsCoreModuleApkVersionName: ")
        return null
    }

    @JavascriptInterface
    fun getGmsCoreModuleVersion(): Int {
        Log.d(TAG, "getGmsCoreModuleVersion: ")
        return 0
    }

    @JavascriptInterface
    fun getGmsCoreVersion(): Int {
        Log.d(TAG, "getGmsCoreVersion: ")
        return Constants.GMS_VERSION_CODE
    }

    @JavascriptInterface
    fun getOsVersion(): String? {
        Log.d(TAG, "getOsVersion: ")
        return RELEASE
    }

    @JavascriptInterface
    fun getSdkVersion(): Int {
        Log.d(TAG, "getSdkVersion: ")
        return SDK_INT
    }

}