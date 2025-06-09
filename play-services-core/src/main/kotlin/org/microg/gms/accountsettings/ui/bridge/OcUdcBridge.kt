/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.accountsettings.ui.bridge

import android.accounts.Account
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import org.microg.gms.accountsettings.ui.evaluateJavascriptCallback
import java.util.Locale

class OcUdcBridge(val webView: WebView) {

    companion object {
        const val NAME = "ocUdc"
        private const val TAG = "JS_$NAME"
    }

    @JavascriptInterface
    fun canGetUlrDeviceInformation(): Boolean {
        Log.d(TAG, "canGetUlrDeviceInformation: ")
        return false
    }

    @JavascriptInterface
    fun canOpenUlrSettingsUi(account: Account?): Boolean {
        Log.d(TAG, "canOpenUlrSettingsUi: account: ${account?.name}")
        return account != null
    }

    @JavascriptInterface
    fun getDeviceSettingsStates(iArray: IntArray, eventId: Int?) {
        Log.d(TAG, "getDeviceSettingsStates: eventId: $eventId")
        ocUdcCallbackError(eventId)
    }

    @JavascriptInterface
    fun getSupportedDeviceSettings(eventId: Int?) {
        Log.d(TAG, "getSupportedDeviceSettings: eventId: $eventId")
        ocUdcCallbackError(eventId)
    }

    @JavascriptInterface
    fun getUlrDeviceInformation(eventId: Int?) {
        Log.d(TAG, "getUlrDeviceInformation: eventId: $eventId")
    }

    @JavascriptInterface
    fun openUlrSettingsUi(): Boolean {
        Log.d(TAG, "openUlrSettingsUi: ")
        return false
    }

    @JavascriptInterface
    fun setDeviceSetting(settingId: Int?, flag: Boolean?, eventId: Int?) {
        Log.d(TAG, "setDeviceSetting: settingId: $settingId, flag: $flag, eventId: $eventId")
        ocUdcCallbackError(eventId)
    }

    private fun ocUdcCallbackError(eventId: Int?) {
        val format = String.format(Locale.ROOT, "window.ocUdcCallback(%s, %s, %s)", eventId, null, true)
        evaluateJavascriptCallback(webView, format)
    }

}