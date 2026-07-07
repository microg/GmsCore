/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.folsom.ui

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.folsom.SharedKey
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import org.microg.gms.auth.folsom.Keys
import org.microg.gms.auth.folsom.ui.GenericActivity.Companion.EXTRA_ACCOUNT_NAME
import org.microg.gms.auth.folsom.ui.GenericActivity.Companion.EXTRA_OFFER_RESET
import org.microg.gms.auth.folsom.ui.GenericActivity.Companion.EXTRA_OPERATION
import org.microg.gms.auth.folsom.ui.GenericActivity.Companion.EXTRA_SECURITY_DOMAIN
import org.microg.gms.auth.folsom.ui.GenericActivity.Companion.EXTRA_SESSION_ID
import org.microg.gms.auth.folsom.utils.LocalKeyManager
import java.util.concurrent.atomic.AtomicBoolean

data class KeyRetrievalMetadata(
    val keys: Map<String, List<SharedKey>>,
    val consent: Map<String, Boolean>
)

class FolsomWebFragment : Fragment() {

    companion object {
        fun newInstance(
            accountName: String,
            securityDomain: String,
            operation: Int = 0,
            sessionId: String = "",
            offerReset: Boolean = false
        ): FolsomWebFragment = FolsomWebFragment().apply {
            arguments = Bundle().apply {
                putString(EXTRA_ACCOUNT_NAME, accountName)
                putString(EXTRA_SECURITY_DOMAIN, securityDomain)
                putInt(EXTRA_OPERATION, operation)
                putString(EXTRA_SESSION_ID, sessionId)
                putBoolean(EXTRA_OFFER_RESET, offerReset)
            }
        }
    }

    private var webView: WebView? = null
    private var progressBar: ProgressBar? = null
    private var jsBridge: FolsomJsBridge? = null
    private var webViewHelper: FolsomWebViewHelper? = null

    private val accountName: String by lazy {
        requireArguments().getString(EXTRA_ACCOUNT_NAME, "")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return setupContainerView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBackPressedHandler()
        setupWebView()
    }

    private fun setupContainerView(): FrameLayout {
        val container = FrameLayout(requireContext())
        container.setBackgroundColor(if (isDarkMode()) Color.BLACK else Color.WHITE)

        progressBar = ProgressBar(requireContext()).also { pb ->
            val size = (48 * resources.displayMetrics.density).toInt()
            val params = FrameLayout.LayoutParams(size, size).apply { gravity = Gravity.CENTER }
            container.addView(pb, params)
        }

        return container
    }

    private fun setupBackPressedHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView?.canGoBack() == true) {
                    webView?.goBack()
                } else {
                    requireActivity().setResult(AppCompatActivity.RESULT_CANCELED)
                    requireActivity().finish()
                }
            }
        })
    }

    private fun setupWebView() {
        val container = view as? FrameLayout ?: return
        val wv = WebView(requireContext()).apply {
            isVisible = false
        }
        container.addView(
            wv, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        webViewHelper = FolsomWebViewHelper(this, wv, accountName).also { helper ->
            helper.prepareWebViewSettings()
            helper.setupWebViewClient(
                onPageStarted = { onPageStarted() },
                onPageFinished = { onPageFinished() }
            )
        }
        jsBridge = FolsomJsBridge(wv, ::onKeyRetrievalResult)
        wv.addJavascriptInterface(jsBridge!!, "mm")
        webView = wv
        loadKeyRetrievalUrl()
    }

    private fun loadKeyRetrievalUrl() {
        if (Build.VERSION.SDK_INT < 21) {
            finishWithResult(null)
            return
        }

        val args = requireArguments()
        val targetUrl = webViewHelper?.buildKeyRetrievalUrl(
            sessionId = args.getString(EXTRA_SESSION_ID, ""),
            securityDomain = args.getString(EXTRA_SECURITY_DOMAIN, ""),
            offerReset = true,
            darkMode = isDarkMode()
        ) ?: return

        webViewHelper?.loadUrlWithAuthentication(targetUrl)
    }

    private fun isDarkMode(): Boolean =
        resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    private fun onPageStarted() {
        progressBar?.isVisible = true
        webView?.isVisible = false
    }

    private fun onPageFinished() {
        progressBar?.isVisible = false
        webView?.isVisible = true
    }

    private fun onKeyRetrievalResult(status: Int, metadata: KeyRetrievalMetadata?) {
        metadata?.let { data ->
            val localKeyManager = LocalKeyManager.getInstance(requireContext())
            data.keys.forEach { (domain, keyList) ->
                if (keyList.isNotEmpty()) {
                    val keysToSave = keyList.map { sharedKey ->
                        Keys(keyVersion = sharedKey.key, keyMaterial = sharedKey.keyMaterial?.toByteString())
                    }
                    localKeyManager.saveKeysForDomain(accountName, domain, keysToSave)
                }
            }
        }
        finishWithResult(if (status == AppCompatActivity.RESULT_OK) status else null)
    }

    private fun finishWithResult(status: Int?) {
        requireActivity().setResult(status ?: AppCompatActivity.RESULT_CANCELED)
        requireActivity().finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        webView?.stopLoading()
        webView?.destroy()
        webView = null
        webViewHelper?.destroy()
        webViewHelper = null
        jsBridge = null
        progressBar = null
    }
}

private class FolsomJsBridge(
    private val webView: WebView,
    private val onResult: (Int, KeyRetrievalMetadata?) -> Unit
) {
    private val keysSet = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val collectedKeys = mutableMapOf<String, List<SharedKey>>()
    private val collectedConsent = mutableMapOf<String, Boolean>()

    @JavascriptInterface
    fun setVaultSharedKeys(accountId: String, keysJson: String) {
        Log.d("FolsomJsBridge", "setVaultSharedKeys called: accountId=$accountId, keysJson=${keysJson.take(200)}")
        runCatching {
            JSONObject(keysJson).let { json ->
                json.keys().forEach { domain ->
                    collectedKeys[domain] = buildList {
                        json.getJSONArray(domain).let { arr ->
                            (0 until arr.length()).forEach { i ->
                                arr.getJSONObject(i).let { keyObj ->
                                    parseKeyMaterial(keyObj.getJSONObject("key"))?.let { km ->
                                        add(SharedKey(keyObj.getInt("epoch"), km))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            keysSet.set(true)
            notifyWebView(0)
        }.onFailure { notifyWebView(1) }
    }

    @JavascriptInterface
    fun setConsent(accountId: String, domain: String, consent: Boolean) {
        Log.d("FolsomJsBridge", "setConsent called: accountId=$accountId, domain=$domain, consent=$consent")
        collectedConsent[domain] = consent
        notifyWebView(0)
    }

    @JavascriptInterface
    fun closeView() {
        val status = if (keysSet.get()) AppCompatActivity.RESULT_OK else AppCompatActivity.RESULT_CANCELED
        val metadata = keysSet.takeIf { it.get() }?.let {
            KeyRetrievalMetadata(collectedKeys.toMap(), collectedConsent.toMap())
        }
        onResult(status, metadata)
    }

    private fun parseKeyMaterial(keyData: JSONObject): ByteArray? = runCatching {
        keyData.optString("keyMaterial").takeIf { !it.isNullOrEmpty() }?.let {
            Base64.decode(it, Base64.DEFAULT)
        } ?: if (keyData.length() > 0) {
            ByteArray(keyData.length()) { i -> keyData.getInt(i.toString()).toByte() }
        } else null
    }.getOrNull()

    private fun notifyWebView(status: Int) {
        mainHandler.post {
            webView.loadUrl("javascript:window.onKeyDataSet(${JSONObject().put("status", status)})")
        }
    }
}