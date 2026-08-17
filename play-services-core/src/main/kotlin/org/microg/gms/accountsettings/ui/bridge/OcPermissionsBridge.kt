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

class OcPermissionsBridge(val webView: WebView) {

    companion object {
        const val NAME = "ocPermissions"
        private const val TAG = "JS_$NAME"
    }

    @JavascriptInterface
    fun checkPermissions(permissionBase64: String?): String? {
        Log.d(TAG, "checkPermissions: permissionBase64: $permissionBase64")
        return null
    }

    @JavascriptInterface
    fun ensurePermissions(permissionBase64: String?) {
        Log.d(TAG, "ensurePermissions: permissionBase64: $permissionBase64")
        val format = String.format(Locale.ROOT, "window.ocPermissionsEnsurePermissionsCallback(%s, %s)", null, true)
        evaluateJavascriptCallback(webView, format)
    }

}