/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.accountsettings.ui.bridge

import android.text.TextUtils
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.gms.R
import org.microg.gms.accountsettings.ui.runOnMainLooper

class OcAppBarBridge(val toolBar: Toolbar, val webView: WebView) {

    companion object {
        const val NAME = "ocAppBar"
        private const val TAG = "JS_$NAME"
    }

    @JavascriptInterface
    fun clear() {
        Log.d(TAG, "clear: ")
        setTitleText(null)
        setTitleType(1)
        setTitleFontFamily(0)
        setStyle(1)
        setAccountDisplay(1)
        setUpButtonAction(1)
        setHelpContext(null)
        setActionMenu(null)
        setShadowVisible(true)
        setUpButtonVisible(true)
        setPullToRefreshEnabled(true)
    }

    @JavascriptInterface
    fun commitChanges() {
        Log.d(TAG, "commitChanges: ")
    }

    @JavascriptInterface
    fun show(id: Double?) {
        Log.d(TAG, "show: id: $id")
    }

    @JavascriptInterface
    fun hide(id: Double?) {
        Log.d(TAG, "hide: id: $id")
    }

    @JavascriptInterface
    fun isNewAppBarFeaturesSupported(): Boolean {
        Log.d(TAG, "isNewAppBarFeaturesSupported: ")
        return true
    }

    @JavascriptInterface
    fun setAccountDisplay(displayId: Int?) {
        Log.d(TAG, "setAccountDisplay: displayId: $displayId")
    }

    @JavascriptInterface
    fun setActionMenu(base64Str: String?) {
        Log.d(TAG, "setActionMenu: base64Str: $base64Str")
    }

    @JavascriptInterface
    fun setHelpContext(url: String?) {
        Log.d(TAG, "setHelpContext: url: $url")
    }

    @JavascriptInterface
    fun setPullToRefreshEnabled(enable: Boolean?) {
        Log.d(TAG, "setPullToRefreshEnabled: enable: $enable")
    }

    @JavascriptInterface
    fun setShadowVisible(visible: Boolean?) {
        Log.d(TAG, "setShadowVisible: visible: $visible")
    }

    @JavascriptInterface
    fun setStyle(style: Int?) {
        Log.d(TAG, "setStyle: style: $style")
    }

    @JavascriptInterface
    fun setTitleFontFamily(family: Int?) {
        Log.d(TAG, "setTitleFontFamily: family: $family")
    }

    @JavascriptInterface
    fun setTitleText(title: String?) {
        Log.d(TAG, "setTitleText: title: $title")
        runOnMainLooper {
            val text = if (TextUtils.isEmpty(title)) {
                ContextCompat.getString(toolBar.context, R.string.pref_accounts_title)
            } else title
            toolBar.setCustomTitleText(text)
        }
    }

    @JavascriptInterface
    fun setTitleType(type: Int?) {
        Log.d(TAG, "setTitleType: type: $type")
    }

    @JavascriptInterface
    fun setUpButtonAction(action: Int?) {
        Log.d(TAG, "setUpButtonAction: action: $action")
    }

    @JavascriptInterface
    fun setUpButtonVisible(visible: Boolean?) {
        Log.d(TAG, "setUpButtonVisible: visible: $visible")
    }

    private fun Toolbar.setCustomTitleText(text: String?) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is TextView) {
                child.text = text
                break
            }
        }
    }
}