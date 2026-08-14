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

class OcTrustAgentBridge(val webView: WebView) {

    companion object {
        const val NAME = "ocTrustAgent"
        private const val TAG = "JS_$NAME"
    }

    @JavascriptInterface
    fun isScreenLockSet(agentId: Int?) {
        Log.d(TAG, "isScreenLockSet: agentId: $agentId")
        ocTrustAgentCallback(agentId, false)
    }

    @JavascriptInterface
    fun isSmartLockSet(agentId: Int?) {
        Log.d(TAG, "isSmartLockSet: agentId: $agentId")
        ocTrustAgentCallback(agentId)
    }

    @JavascriptInterface
    fun isSmartLockSupported(agentId: Int?) {
        Log.d(TAG, "isSmartLockSupported: agentId: $agentId")
        ocTrustAgentCallback(agentId, false)
    }

    @JavascriptInterface
    fun isTrustletSet(config: String?, agentId: Int?) {
        Log.d(TAG, "isTrustletSet: agentId: $agentId")
        ocTrustAgentCallback(agentId)
    }

    @JavascriptInterface
    fun isTrustletSupported(agentId: Int?) {
        Log.d(TAG, "isTrustletSupported: agentId: $agentId")
        ocTrustAgentCallback(agentId)
    }

    @JavascriptInterface
    fun startScreenLockSmartLockFlow(agentId: Int?) {
        Log.d(TAG, "startScreenLockSmartLockFlow: agentId: $agentId")
        ocTrustAgentCallbackV2(agentId)
    }

    private fun ocTrustAgentCallback(agentId: Int?, valid: Boolean) {
        val format = String.format(Locale.ROOT, "window.ocTrustAgentCallback(%s, %s)", agentId, valid)
        evaluateJavascriptCallback(webView, format)
    }

    private fun ocTrustAgentCallback(agentId: Int?) {
        val format = String.format(Locale.ROOT, "window.ocTrustAgentCallback(%s, %s, %s)", agentId, false, true)
        evaluateJavascriptCallback(webView, format)
    }

    private fun ocTrustAgentCallbackV2(agentId: Int?) {
        val format = String.format(Locale.ROOT, "window.ocTrustAgentCallback(%s)", agentId)
        evaluateJavascriptCallback(webView, format)
    }

}