/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.accountsettings.ui.bridge

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import org.microg.gms.checkin.LastCheckinInfo

class OcAndroidIdBridge(val context: Context) {

    companion object {
        const val NAME = "ocAndroidId"
        private const val TAG = "JS_$NAME"
    }

    @OptIn(ExperimentalStdlibApi::class)
    @JavascriptInterface
    fun getAndroidId(): String? {
        Log.d(TAG, "getAndroidId: ")
        val androidId = LastCheckinInfo.read(context).androidId
        return if (androidId != 0L) androidId.toHexString() else null
    }

}