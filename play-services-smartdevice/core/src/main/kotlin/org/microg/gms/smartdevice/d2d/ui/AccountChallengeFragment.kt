/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.smartdevice.d2d.ui

import android.content.Context
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import org.microg.gms.smartdevice.d2d.R
import java.util.Locale

/**
 * Drives source-side account re-authentication challenges in a [WebView].
 *
 * Challenge state is supplied through arguments and completed checkpoints are
 * returned to the host activity.
 */
class AccountChallengeFragment : Fragment() {

    interface Listener {
        /** All challenges in the queue completed; host should publish [checkpoints]. */
        fun onChallengesComplete(checkpoints: ArrayList<Bundle>)
        /** A challenge WebView reported an unrecoverable main-frame error. */
        fun onChallengeError(errorCode: Int)
        /** MagicArch reported the parent account used for supervised authentication. */
        fun onRestoreAccount(account: String?)
        /** The user requested to abort the transfer (e.g. via the back button). */
        fun onCancelRequested()
    }

    private var listener: Listener? = null
    private var pendingWebChallenges: ArrayList<Bundle> = ArrayList()
    private val completed: ArrayList<Bundle> = ArrayList()
    private var index = 0
    private var mainFrameErrorReported = false
    private var startInvoked = false
    private var invalidChallengeData = false
    private var completionDispatched = false
    private var activeChallengeIndex = -1
    private var activeChallengeOrigin: String? = null
    private var activeChallengeConsumed = false
    private var isFidoFlow = false
    private var challengeWebView: WebView? = null
    private var progressBar: ProgressBar? = null
    private var subtitle: TextView? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = (parentFragment as? Listener) ?: (context as? Listener)
            ?: throw IllegalStateException(
                "Containing activity must implement AccountChallengeFragment.Listener"
            )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isFidoFlow = arguments?.getBoolean(ARG_IS_FIDO_FLOW, false) == true
        @Suppress("UNCHECKED_CAST")
        val raw = (savedInstanceState?.getParcelableArrayList<Bundle>(STATE_PENDING_WEB_CHALLENGES)
            ?: arguments?.getParcelableArrayList<Bundle>(ARG_ACCOUNTS)
            ?: ArrayList<Bundle>())
        pendingWebChallenges = ArrayList(raw)
        invalidChallengeData = isFidoFlow && raw.any { challenge ->
            !isHttpsUrl(challenge.getString(KEY_URL)) ||
                challenge.getString(KEY_NAME).isNullOrEmpty()
        }
        index = savedInstanceState?.getInt(STATE_CHALLENGE_INDEX, 0) ?: 0
        completionDispatched = savedInstanceState?.getBoolean(STATE_COMPLETION_DISPATCHED) == true
        savedInstanceState?.getParcelableArrayList<Bundle>(STATE_COMPLETED_CHALLENGES)
            ?.let(completed::addAll)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.d2d_account_challenge, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.d2d_challenge_back).setOnClickListener {
            listener?.onCancelRequested()
        }
        subtitle = view.findViewById(R.id.d2d_challenge_subtitle)
        val challengeProgress = view.findViewById<ProgressBar>(R.id.d2d_challenge_progress)
        tintProgressBar(challengeProgress)
        progressBar = challengeProgress
        val webView = view.findViewById<WebView>(R.id.d2d_challenge_webview)
        configureWebView(webView)
        challengeWebView = webView
    }

    override fun onResume() {
        super.onResume()
        if (startInvoked) return
        startInvoked = true
        if (invalidChallengeData) {
            listener?.onChallengeError(ERROR_INVALID_CHALLENGE_DATA)
            return
        }
        if (pendingWebChallenges.isEmpty()) {
            dispatchCompletion(ArrayList())
            return
        }
        finishOrLoadNext()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(STATE_PENDING_WEB_CHALLENGES, pendingWebChallenges)
        outState.putInt(STATE_CHALLENGE_INDEX, index)
        outState.putParcelableArrayList(STATE_COMPLETED_CHALLENGES, completed)
        outState.putBoolean(STATE_COMPLETION_DISPATCHED, completionDispatched)
    }

    override fun onDestroyView() {
        challengeWebView?.let {
            it.stopLoading()
            it.webChromeClient = null
            it.webViewClient = WebViewClient()
            it.removeAllViews()
            it.destroy()
        }
        challengeWebView = null
        progressBar = null
        subtitle = null
        super.onDestroyView()
    }

    override fun onDetach() {
        listener = null
        super.onDetach()
    }

    /**
     * Replaces the pending challenge queue and restarts from the first item.
     * If the view is not ready, the queue is consumed on the next resume.
     */
    fun updateChallengeData(newChallenges: ArrayList<Bundle>) {
        pendingWebChallenges = ArrayList(newChallenges)
        invalidChallengeData = isFidoFlow && newChallenges.any { challenge ->
            !isHttpsUrl(challenge.getString(KEY_URL)) ||
                challenge.getString(KEY_NAME).isNullOrEmpty()
        }
        index = 0
        completed.clear()
        completionDispatched = false
        activeChallengeIndex = -1
        activeChallengeOrigin = null
        activeChallengeConsumed = false
        mainFrameErrorReported = false
        // Clear stale page state before starting the replacement queue.
        challengeWebView?.let {
            it.stopLoading()
            it.clearHistory()
        }
        progressBar?.visibility = View.VISIBLE
        if (isAdded && !isDetached && view != null) {
            startInvoked = true
            if (invalidChallengeData) {
                listener?.onChallengeError(ERROR_INVALID_CHALLENGE_DATA)
            } else {
                finishOrLoadNext()
            }
        }
    }

    /** Restarts the current challenge after a configuration change. */
    fun reload() {
        if (pendingWebChallenges.isEmpty()) return
        if (index >= pendingWebChallenges.size) {
            dispatchCompletion(ArrayList(completed))
            return
        }
        mainFrameErrorReported = false
        finishOrLoadNext()
    }

    private fun configureWebView(webView: WebView) {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.setSupportZoom(false)
        webView.settings.builtInZoomControls = false
        webView.settings.displayZoomControls = false
        webView.settings.saveFormData = false
        webView.settings.allowFileAccess = false
        webView.settings.allowContentAccess = false
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        webView.isFocusableInTouchMode = true
        CookieManager.getInstance().setAcceptCookie(true)
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progressBar?.apply {
                    isIndeterminate = false
                    progress = newProgress
                    visibility = if (newProgress < 100) View.VISIBLE else View.INVISIBLE
                }
            }
        }
        webView.webViewClient = object : WebViewClient() {
            @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean = handleNavigation(url)

            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                mainFrameErrorReported = false
                progressBar?.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String) {
                if (index >= pendingWebChallenges.size || mainFrameErrorReported || view.url != url) return
                val expectedIndex = activeChallengeIndex
                if (expectedIndex != index || (isFidoFlow && !isTrustedActiveUrl(url))) return
                val cookies = ChallengeCookieReader.read(url)
                if (isFidoFlow && cookies.oauthToken != null) {
                    completeCurrentFidoChallenge(expectedIndex)
                    return
                }
                val checkpoint = cookies.sessionCheckpoint ?: return
                completed += Bundle(pendingWebChallenges[index]).apply {
                    putString(KEY_SESSION_CHECKPOINT, checkpoint)
                    putString(KEY_URL, null)
                }
                index++
                finishOrLoadNext()
            }

            @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
            override fun onReceivedError(view: WebView, errorCode: Int, description: String?, failingUrl: String?) {
                if (android.os.Build.VERSION.SDK_INT < 23) handleMainFrameError(errorCode)
            }

            @RequiresApi(23)
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) handleMainFrameError(error.errorCode)
            }
        }
        webView.requestFocus(View.FOCUS_DOWN)
    }

    private fun finishOrLoadNext() {
        if (index >= pendingWebChallenges.size) {
            dispatchCompletion(ArrayList(completed))
            return
        }
        val item = pendingWebChallenges[index]
        val url = item.getString(KEY_URL)
        if (url.isNullOrEmpty()) {
            Log.d(TAG, "Url is empty.")
            index++
            finishOrLoadNext()
            return
        }
        val accountName = item.getString(KEY_NAME)?.takeIf(String::isNotBlank)
            ?: getString(R.string.d2d_unknown_account)
        subtitle?.text = getString(
            R.string.d2d_verify_account,
            index + 1,
            pendingWebChallenges.size,
            accountName,
        )
        var uri = Uri.parse(url)
        val requestedLocale = uri.getQueryParameter(LOCALE_QUERY_PARAMETER)
        val deviceLocale = Locale.getDefault().toString()
        if (!requestedLocale.isNullOrEmpty() && requestedLocale != deviceLocale) {
            val localized = uri.buildUpon().clearQuery()
            uri.queryParameterNames.forEach { name ->
                if (name != LOCALE_QUERY_PARAMETER) localized.appendQueryParameter(name, uri.getQueryParameter(name))
            }
            uri = localized.appendQueryParameter(LOCALE_QUERY_PARAMETER, deviceLocale).build()
        }
        val showSkip = arguments?.getBoolean(ARG_SHOW_SKIP_ACCOUNT) ?: true
        val statusBarBg = usesGlifSetupWizardTheme()
        val challengeUri = uri.buildUpon().apply {
            if (showSkip) appendQueryParameter(SHOW_SKIP_ACCOUNT_QUERY_PARAMETER, "1")
            if (statusBarBg) appendQueryParameter(SHOW_STATUS_BAR_BACKGROUND_QUERY_PARAMETER, "1")
        }.build()
        activeChallengeIndex = index
        activeChallengeOrigin = challengeUri.origin()
        activeChallengeConsumed = false
        if (isFidoFlow) {
            challengeWebView?.removeJavascriptInterface(MAGIC_ARCH_INTERFACE)
            challengeWebView?.addJavascriptInterface(
                MagicArchJavascriptBridge(activeChallengeIndex, checkNotNull(activeChallengeOrigin)),
                MAGIC_ARCH_INTERFACE,
            )
        }
        progressBar?.apply {
            visibility = View.VISIBLE
            isIndeterminate = true
        }
        clearCookies {
            if (isAdded && !isDetached) {
                challengeWebView?.loadUrl(challengeUri.toString())
            }
        }
    }

    private fun handleNavigation(url: String): Boolean {
        val expectedIndex = activeChallengeIndex
        if (isFidoFlow &&
            isTrustedActiveUrl(url) &&
            Uri.parse(url).getQueryParameter(WEBVIEW_ACTION_QUERY_PARAMETER) == WEBVIEW_ACTION_CLOSE
        ) {
            completeCurrentFidoChallenge(expectedIndex)
            return true
        }
        val skipCurrentAccount = runCatching {
            val value = Uri.parse(url).getQueryParameter(DO_SKIP_ACCOUNT_QUERY_PARAMETER)
            !value.isNullOrEmpty() && value.all(Char::isDigit) && value.toIntOrNull() == 1
        }.getOrDefault(false)
        if (!skipCurrentAccount) return false
        skipCurrentChallenge(expectedIndex)
        return true
    }

    private fun completeCurrentFidoChallenge(expectedIndex: Int) {
        if (!consumeActiveChallenge(expectedIndex)) return
        completed += Bundle(pendingWebChallenges[index])
        index++
        finishOrLoadNext()
    }

    private fun skipCurrentChallenge(expectedIndex: Int) {
        if (!consumeActiveChallenge(expectedIndex)) return
        index++
        finishOrLoadNext()
    }

    private inner class MagicArchJavascriptBridge(
        private val expectedIndex: Int,
        private val expectedOrigin: String,
    ) {
        @JavascriptInterface
        fun finishChallenge() {
            challengeWebView?.post {
                if (isTrustedActiveOrigin(expectedOrigin)) completeCurrentFidoChallenge(expectedIndex)
            }
        }

        @JavascriptInterface
        fun skipAccount() {
            challengeWebView?.post {
                if (isTrustedActiveOrigin(expectedOrigin)) skipCurrentChallenge(expectedIndex)
            }
        }

        @JavascriptInterface
        fun setAuthenticatingParentId(parentId: String?) {
            parentId?.takeIf(String::isNotBlank)?.let { value ->
                challengeWebView?.post {
                    if (index == expectedIndex && isTrustedActiveOrigin(expectedOrigin)) {
                        listener?.onRestoreAccount(value)
                    }
                }
            }
        }
    }

    private fun consumeActiveChallenge(expectedIndex: Int): Boolean {
        if (expectedIndex < 0 || expectedIndex != index || expectedIndex != activeChallengeIndex) return false
        if (activeChallengeConsumed || index >= pendingWebChallenges.size) return false
        activeChallengeConsumed = true
        challengeWebView?.stopLoading()
        return true
    }

    private fun isTrustedActiveUrl(url: String?): Boolean =
        url?.let { isHttpsUrl(it) && Uri.parse(it).origin() == activeChallengeOrigin } == true

    private fun isTrustedActiveOrigin(expectedOrigin: String): Boolean =
        expectedOrigin == activeChallengeOrigin && isTrustedActiveUrl(challengeWebView?.url)

    private fun dispatchCompletion(result: ArrayList<Bundle>) {
        if (completionDispatched) return
        completionDispatched = true
        listener?.onChallengesComplete(result)
    }

    private fun handleMainFrameError(errorCode: Int) {
        if (mainFrameErrorReported) return
        mainFrameErrorReported = true
        Log.e(TAG, "Account challenge WebView failed code=$errorCode")
        listener?.onChallengeError(errorCode)
    }

    private fun clearCookies(onComplete: () -> Unit) {
        val cookies = CookieManager.getInstance()
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            cookies.removeAllCookies { onComplete() }
        } else {
            @Suppress("DEPRECATION")
            cookies.removeAllCookie()
            onComplete()
        }
    }

    private fun usesGlifSetupWizardTheme(): Boolean {
        val theme = runCatching {
            Class.forName("android.os.SystemProperties")
                .getMethod("get", String::class.java, String::class.java)
                .invoke(null, "setupwizard.theme", "") as? String
        }.getOrNull()
        return theme == "glif" || theme == "glif_light"
    }

    private fun tintProgressBar(target: ProgressBar) {
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            val tint = ColorStateList.valueOf(resources.getColor(R.color.d2d_primary))
            target.indeterminateTintList = tint
            target.progressTintList = tint
        }
    }

    companion object {
        private const val TAG = "AccountChallengeFrag"

        const val ARG_ACCOUNTS = "accounts"
        const val ARG_SHOW_SKIP_ACCOUNT = "showSkipAccount"
        const val ARG_TRANSITION_TITLE = "transitionTitle"
        const val ARG_TRANSITION_DESCRIPTION = "transitionDescription"
        const val ARG_IS_FIDO_FLOW = "isFidoFlow"

        private const val KEY_URL = "url"
        private const val KEY_NAME = "name"
        private const val KEY_SESSION_CHECKPOINT = "sessionCheckpoint"
        private const val DO_SKIP_ACCOUNT_QUERY_PARAMETER = "doSkipAccount"
        private const val SHOW_SKIP_ACCOUNT_QUERY_PARAMETER = "showSkipAccount"
        private const val SHOW_STATUS_BAR_BACKGROUND_QUERY_PARAMETER = "showStatusBarBg"
        private const val LOCALE_QUERY_PARAMETER = "hl"
        private const val WEBVIEW_ACTION_QUERY_PARAMETER = "wv_action"
        private const val WEBVIEW_ACTION_CLOSE = "close"
        private const val MAGIC_ARCH_INTERFACE = "mm"

        private const val STATE_PENDING_WEB_CHALLENGES = "accounts"
        private const val STATE_CHALLENGE_INDEX = "challengeIndex"
        private const val STATE_COMPLETED_CHALLENGES = "completedChallenges"
        private const val STATE_COMPLETION_DISPATCHED = "completionDispatched"
        private const val ERROR_INVALID_CHALLENGE_DATA = 0

        fun newInstance(
            challengeData: ArrayList<Bundle>,
            showSkipAccount: Boolean = true,
            isFidoFlow: Boolean = false,
            transitionTitle: String? = null,
            transitionDescription: String? = null,
        ): AccountChallengeFragment = AccountChallengeFragment().apply {
            arguments = Bundle().apply {
                putParcelableArrayList(ARG_ACCOUNTS, challengeData)
                putBoolean(ARG_SHOW_SKIP_ACCOUNT, showSkipAccount)
                putBoolean(ARG_IS_FIDO_FLOW, isFidoFlow)
                if (!transitionTitle.isNullOrBlank()) putString(ARG_TRANSITION_TITLE, transitionTitle)
                if (!transitionDescription.isNullOrBlank()) putString(ARG_TRANSITION_DESCRIPTION, transitionDescription)
            }
        }
    }
}

private object ChallengeCookieReader {
    fun read(url: String): ChallengeCookies {
        var oauthToken: String? = null
        var checkpoint: String? = null
        CookieManager.getInstance().getCookie(url)
            ?.split(';')
            ?.map(String::trim)
            ?.forEach { cookie ->
                val separator = cookie.indexOf('=')
                if (separator <= 0 || separator + 1 >= cookie.length) return@forEach
                val name = cookie.substring(0, separator).trim()
                val value = cookie.substring(separator + 1).trim()
                when {
                    name.equals(OAUTH_TOKEN_COOKIE, ignoreCase = true) -> {
                        expire(url, OAUTH_TOKEN_COOKIE)
                        oauthToken = value
                    }
                    name.equals(SESSION_CHECKPOINT_COOKIE, ignoreCase = true) -> {
                        expire(url, SESSION_CHECKPOINT_COOKIE)
                        checkpoint = value.takeIf(String::isNotEmpty)
                    }
                }
                if (oauthToken != null && checkpoint != null) {
                    return ChallengeCookies(oauthToken, checkpoint)
                }
            }
        return ChallengeCookies(oauthToken, checkpoint)
    }

    private fun expire(url: String, name: String) {
        CookieManager.getInstance().setCookie(
            url,
            "$name=; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/; Secure",
        )
        if (android.os.Build.VERSION.SDK_INT >= 21) CookieManager.getInstance().flush()
    }

    private const val OAUTH_TOKEN_COOKIE = "oauth_token"
    private const val SESSION_CHECKPOINT_COOKIE = "GASC"
}

private data class ChallengeCookies(
    val oauthToken: String?,
    val sessionCheckpoint: String?,
)

private fun isHttpsUrl(url: String?): Boolean = runCatching {
    val uri = Uri.parse(url)
    uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
}.getOrDefault(false)

private fun Uri.origin(): String? {
    val scheme = scheme?.lowercase(Locale.US) ?: return null
    val host = host?.lowercase(Locale.US) ?: return null
    val effectivePort = if (port >= 0) port else if (scheme == "https") 443 else -1
    return "$scheme://$host:$effectivePort"
}
