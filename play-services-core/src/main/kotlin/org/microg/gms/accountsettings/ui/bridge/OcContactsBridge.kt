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

class OcContactsBridge(val webView: WebView) {

    companion object {
        const val NAME = "ocContacts"
        private const val TAG = "JS_$NAME"
    }

    @JavascriptInterface
    fun readContacts() {
        Log.d(TAG, "readContacts: ")
        val format = String.format(Locale.ROOT, "window.ocContactsReadContactsCallback(%s, %s)", null, true)
        evaluateJavascriptCallback(webView, format)
    }
}