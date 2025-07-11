/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.accountsettings.ui.bridge

import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import org.microg.gms.accountsettings.ui.evaluateJavascriptCallback
import java.util.Locale

class OcPlayProtectBridge(val webView: WebView) {

    companion object {
        const val NAME = "ocPlayProtect"
        private const val TAG = "JS_$NAME"
    }

    @JavascriptInterface
    fun enablePlayProtect(protectId: Int?) {
        Log.d(TAG, "enablePlayProtect: protectId: $protectId")
        ocPlayProtectCallback(protectId)
    }

    @JavascriptInterface
    fun getHarmfulAppsCount(protectId: Int?) {
        Log.d(TAG, "getHarmfulAppsCount: protectId: $protectId")
        ocPlayProtectCallback(protectId)
    }

    @JavascriptInterface
    fun getLastScanTimeMs(protectId: Int?) {
        Log.d(TAG, "getLastScanTimeMs: protectId: $protectId")
        ocPlayProtectCallback(protectId)
    }

    @JavascriptInterface
    fun isPlayProtectEnabled(protectId: Int?) {
        Log.d(TAG, "isPlayProtectEnabled: protectId: $protectId")
        ocPlayProtectCallback(protectId)
    }

    @JavascriptInterface
    fun isPlayStoreVersionValid(protectId: Int?) {
        Log.d(TAG, "isPlayStoreVersionValid: protectId: $protectId")
        ocPlayProtectCallback(protectId, true)
    }

    @JavascriptInterface
    fun startPlayProtectActivity(protectId: Int?) {
        Log.d(TAG, "startPlayProtectActivity: protectId: $protectId")
        ocPlayProtectCallbackV2(protectId)
    }

    private fun ocPlayProtectCallback(protectId: Int?, valid: Boolean) {
        val format = String.format(Locale.ROOT, "window.ocPlayProtectCallback(%s, %s)", protectId, valid)
        evaluateJavascriptCallback(webView, format)
    }

    private fun ocPlayProtectCallback(protectId: Int?) {
        val format = String.format(Locale.ROOT, "window.ocPlayProtectCallback(%s, %s, %s)", protectId, null, true)
        evaluateJavascriptCallback(webView, format)
    }

    private fun ocPlayProtectCallbackV2(protectId: Int?) {
        val format = String.format(Locale.ROOT, "window.ocPlayProtectCallback(%s)", protectId)
        evaluateJavascriptCallback(webView, format)
    }

}