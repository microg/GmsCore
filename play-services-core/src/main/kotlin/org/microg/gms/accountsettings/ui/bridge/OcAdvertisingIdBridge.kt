/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.accountsettings.ui.bridge

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import com.google.android.gms.ads.identifier.AdvertisingIdClient

class OcAdvertisingIdBridge(val context: Context) {

    companion object {
        const val NAME = "ocAdvertisingId"
        private const val TAG = "JS_$NAME"
    }

    @JavascriptInterface
    fun getAdvertisingId(): String? {
        Log.d(TAG, "getAdvertisingId: ")
        return AdvertisingIdClient.getAdvertisingIdInfo(context).id
    }

}