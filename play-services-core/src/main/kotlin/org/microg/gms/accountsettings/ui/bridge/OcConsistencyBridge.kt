/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.accountsettings.ui.bridge

import android.util.Log
import android.webkit.JavascriptInterface

class OcConsistencyBridge() {

    companion object {
        const val NAME = "ocConsistency"
        private const val TAG = "JS_$NAME"
    }

    @JavascriptInterface
    fun accountWasDeleted() {
        Log.d(TAG, "accountWasDeleted: ")
    }

    @JavascriptInterface
    fun accountWasRenamed() {
        Log.d(TAG, "accountWasRenamed: ")
    }

    @JavascriptInterface
    fun verifyActualAccountId(accountId: String?) {
        Log.d(TAG, "verifyActualAccountId: accountId: $accountId")
    }

}