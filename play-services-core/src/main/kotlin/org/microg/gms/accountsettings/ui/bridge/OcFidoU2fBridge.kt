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

class OcFidoU2fBridge(val webView: WebView) {

    companion object {
        const val NAME = "mm"
        private const val TAG = "JS_$NAME"
    }

    @JavascriptInterface
    fun sendSkUiEvent(eventJsonStr: String?) {
        Log.d(TAG, "sendSkUiEvent: eventJsonStr: $eventJsonStr")
    }

    @JavascriptInterface
    fun startSecurityKeyAssertionRequest(requestJsonStr: String?) {
        Log.d(TAG, "startSecurityKeyAssertionRequest: requestJsonStr: $requestJsonStr")
        val format = String.format(Locale.ROOT, "window.setSkResult(%s);", null)
        evaluateJavascriptCallback(webView, format)
    }
}