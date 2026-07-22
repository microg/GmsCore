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

class OcFido2Bridge(val webView: WebView) {

    companion object {
        const val NAME = "ocFido2"
        private const val TAG = "JS_$NAME"
    }

    @JavascriptInterface
    fun startBuiltInAuthenticatorAssertionRequest(json: String?) {
        Log.d(TAG, "startBuiltInAuthenticatorAssertionRequest: json: $json")
        val format = String.format(Locale.ROOT, "window.ocFido2BuiltInAuthenticatorAssertionResponse(%s)", null)
        evaluateJavascriptCallback(webView, format)
    }

}