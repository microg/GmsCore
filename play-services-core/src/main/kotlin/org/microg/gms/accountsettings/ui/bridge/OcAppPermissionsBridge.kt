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

class OcAppPermissionsBridge(val webView: WebView) {

    companion object {
        const val NAME = "ocAppPermissions"
        private const val TAG = "JS_$NAME"
    }

    @JavascriptInterface
    fun getAppPermissionsData(eventId: Int?) {
        Log.d(TAG, "getAppPermissionsData: eventId: $eventId")
        ocAppPermissionsCallbackError(eventId)
    }

    @JavascriptInterface
    fun getSupportedPermissionsDescription(eventId: Int?) {
        Log.d(TAG, "getSupportedPermissionsDescription: eventId: $eventId")
        ocAppPermissionsCallbackError(eventId)
    }

    private fun ocAppPermissionsCallbackError(eventId: Int?) {
        val format = String.format(Locale.ROOT, "window.ocAppPermissionsCallback(%s, %s, %s)", eventId, null, true)
        evaluateJavascriptCallback(webView, format)
    }

}