/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.accountsettings.ui.bridge

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import org.json.JSONObject
import org.microg.gms.accountsettings.AccountSettingsHelpParams
import org.microg.gms.accountsettings.ui.EXTRA_SCREEN_OPTIONS_PREFIX
import org.microg.gms.accountsettings.ui.KEY_UPDATED_PHOTO_URL
import org.microg.gms.accountsettings.ui.MainActivity
import org.microg.gms.accountsettings.ui.finishActivity
import org.microg.gms.accountsettings.ui.getHelpUrl
import org.microg.gms.accountsettings.ui.openScreen
import org.microg.gms.accountsettings.ui.openUrl
import org.microg.gms.accountsettings.ui.runOnMainLooper

class OcUiBridge(val activity: MainActivity, val accountName: String?, val callingPackage: String, val webView: WebView?) {

    companion object{
        const val NAME = "ocUi"
        private const val TAG = "JS_$NAME"
    }

    private var resultBundle: Bundle? = null

    @JavascriptInterface
    fun close() {
        Log.d(TAG, "close: ")
        val intent = Intent()
        if (resultBundle != null) {
            intent.putExtras(resultBundle!!)
        }
        accountName?.let { activity.updateVerifyNotification(it) }
        activity.setResult(RESULT_OK, intent)
        activity.finishActivity()
    }

    @JavascriptInterface
    fun closeWithResult(resultJsonStr: String?) {
        Log.d(TAG, "closeWithResult: resultJsonStr -> $resultJsonStr")
        setResult(resultJsonStr)
        close()
    }

    @JavascriptInterface
    fun goBackOrClose() {
        Log.d(TAG, "goBackOrClose: ")
        runOnMainLooper { activity.onBackPressed() }
    }

    @JavascriptInterface
    fun hideKeyboard() {
        Log.d(TAG, "hideKeyboard: ")
    }

    @JavascriptInterface
    fun isCloseWithResultSupported(): Boolean {
        Log.d(TAG, "isCloseWithResultSupported: ")
        return true
    }

    @JavascriptInterface
    fun isOpenHelpEnabled(): Boolean {
        Log.d(TAG, "isOpenHelpEnabled: ")
        return true
    }

    @JavascriptInterface
    fun isOpenScreenEnabled(): Boolean {
        Log.d(TAG, "isOpenScreenEnabled: ")
        return true
    }

    @JavascriptInterface
    fun isSetResultSupported(): Boolean {
        Log.d(TAG, "isSetResultSupported: ")
        return true
    }

    @JavascriptInterface
    fun open(str: String?) {
        Log.d(TAG, "open: str -> $str")
        runOnMainLooper { activity.openUrl(str, accountName, callingPackage) }
    }

    @JavascriptInterface
    fun openHelp(str: String?) {
        Log.d(TAG, "openHelp: str -> $str")
        val params = runCatching {
            str?.let { AccountSettingsHelpParams.ADAPTER.decode(Base64.decode(it, Base64.DEFAULT)) }
        }.onFailure { Log.w(TAG, "Unable to parse help parameters: ${it.javaClass.simpleName}") }.getOrNull() ?: return
        runOnMainLooper { activity.openUrl(activity.getHelpUrl(params), accountName, callingPackage, openExternally = true) }
    }

    @JavascriptInterface
    fun openScreen(screenId: Int, str: String?) {
        Log.d(TAG, "openScreen: screenId -> $screenId str -> $str accountName -> $accountName")
        val options = jsonToMap(str).orEmpty()
        runOnMainLooper {
            activity.openScreen(screenId, Bundle().apply {
                options.forEach { (key, value) -> putString(EXTRA_SCREEN_OPTIONS_PREFIX + key, value) }
            }, accountName, callingPackage)
        }
    }

    @JavascriptInterface
    fun setBackStop() {
        Log.d(TAG, "setBackStop: ")
        runOnMainLooper { webView?.clearHistory() }
    }

    @JavascriptInterface
    fun setResult(resultJsonStr: String?) {
        Log.d(TAG, "setResult: resultJsonStr -> $resultJsonStr")
        val map = jsonToMap(resultJsonStr) ?: return
        if (map.containsKey(KEY_UPDATED_PHOTO_URL)) {
            activity.updateLocalAccountAvatar(map[KEY_UPDATED_PHOTO_URL], accountName)
        }
        resultBundle = Bundle().apply {
            for ((key, value) in map) {
                putString("result.$key", value)
            }
        }
    }

    private fun jsonToMap(jsonStr: String?): Map<String, String>? {
        val hashMap = HashMap<String, String>()
        if (!jsonStr.isNullOrEmpty()) {
            try {
                val jSONObject = JSONObject(jsonStr)
                val keys = jSONObject.keys()
                while (keys.hasNext()) {
                    val next = keys.next()
                    val obj = jSONObject[next]
                    hashMap[next] = obj as String
                }
            } catch (e: Exception) {
                Log.d(TAG, "Unable to parse result JSON string", e)
                return null
            }
        }
        return hashMap
    }

}