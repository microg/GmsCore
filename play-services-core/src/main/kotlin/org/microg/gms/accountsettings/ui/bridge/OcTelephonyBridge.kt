/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.accountsettings.ui.bridge

import android.util.Log
import android.webkit.JavascriptInterface

class OcTelephonyBridge() {

    companion object {
        const val NAME = "ocTelephony"
        private const val TAG = "JS_$NAME"
    }

    @JavascriptInterface
    fun getPhoneNumber(): String? {
        Log.d(TAG, "getPhoneNumber: ")
        return null
    }

    @JavascriptInterface
    fun getSimCountryIso(): String? {
        Log.d(TAG, "getSimCountryIso: ")
        return null
    }

    @JavascriptInterface
    fun getSimState(): Int {
        Log.d(TAG, "getSimState: ")
        return 0
    }

    @JavascriptInterface
    fun hasPhoneNumber(): Boolean {
        Log.d(TAG, "hasPhoneNumber: ")
        return false
    }

    @JavascriptInterface
    fun hasTelephony(): Boolean {
        Log.d(TAG, "hasTelephony: ")
        return false
    }

    @JavascriptInterface
    fun listenForSmsCodes() {
        Log.d(TAG, "listenForSmsCodes: ")
    }

    @JavascriptInterface
    fun sendSms(type: Int, contentBase64: String) {
        Log.d(TAG, "sendSms: type: $type, contentBase64: $contentBase64")
    }

    @JavascriptInterface
    fun sendSmsSupportedByBridge(): Boolean {
        Log.d(TAG, "sendSmsSupportedByBridge: ")
        return false
    }

    @JavascriptInterface
    fun stopListeningForSmsCodes() {
        Log.d(TAG, "stopListeningForSmsCodes: ")
    }
}