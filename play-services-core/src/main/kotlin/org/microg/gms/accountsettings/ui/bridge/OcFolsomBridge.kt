/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.accountsettings.ui.bridge

import android.util.Log
import android.webkit.JavascriptInterface

class OcFolsomBridge() {

    companion object {
        const val NAME = "ocFolsom"
        private const val TAG = "JS_$NAME"
    }

    @OptIn(ExperimentalStdlibApi::class)
    @JavascriptInterface
    fun addEncryptionRecoveryMethod(key: String?, jsonArray: String?, jsonObject: String?, eventId: Int?) {
        Log.d(TAG, "addEncryptionRecoveryMethod: key: $key, jsonArray: $jsonArray, jsonObject: $jsonObject, eventId: $eventId")
    }

}