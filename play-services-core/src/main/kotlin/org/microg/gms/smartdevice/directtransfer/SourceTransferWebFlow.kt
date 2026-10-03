/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.smartdevice.directtransfer

import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.microg.gms.auth.AuthManager
import org.microg.gms.common.Constants.GMS_PACKAGE_NAME
import org.microg.gms.cryptauth.CryptAuthBootstrapAuthorization
import java.io.ByteArrayInputStream
import java.io.Closeable
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/** Same-process memory only: no web URL, token or result is passed through an Intent/Bundle. */
internal class SourceTransferWebRequest(
    val authorization: CryptAuthBootstrapAuthorization,
    val fallbackUrl: String
) {
    val result = CompletableDeferred<String>()
    fun checkCurrent() { check(!result.isCompleted); authorization.enforceCurrent() }
    fun complete(checkpoint: String) {
        checkCurrent()
        check(SourceTransferWebPolicy.isCheckpointValid(checkpoint))
        check(result.complete(checkpoint))
    }
    fun cancel() { result.cancel() }
    fun fail(reason: SourceTransferDiagnosticReason) {
        result.completeExceptionally(SourceTransferProtocolException(10575, reason))
    }
    override fun toString() = "SourceTransferWebRequest(redacted)"
}

/** Isolated cookie/storage profile and bounded, cancellable account verification. */
internal class SourceTransferWebFlow(
    private val activity: Activity,
    private val request: SourceTransferWebRequest,
    private val layout: LinearLayout,
    private val onCheckpoint: () -> Unit
) : Closeable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val closed = AtomicBoolean()
    private val profileName = "microg_direct_transfer_" + UUID.randomUUID().toString()
    private var webView: WebView? = null
    private var profileAssigned = false
    private var cookies: CookieManager? = null

    fun start() {
        request.checkCurrent()
        check(SourceTransferWebPolicy.isNavigationAllowed(request.fallbackUrl))
        ensureProcessStorage()
        // Shared CookieManager is never a fallback: without isolation this transfer must fail.
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE))
            throw SourceTransferProtocolException(10575, SourceTransferDiagnosticReason.WEB_ISOLATION_UNAVAILABLE)
        // Retire profiles left by process death; never touch another auth flow's storage.
        ProfileStore.getInstance().allProfileNames.filter { it.startsWith("microg_direct_transfer_") }.forEach {
            runCatching { ProfileStore.getInstance().deleteProfile(it) }
        }
        val view = WebView(activity)
        webView = view
        WebViewCompat.setProfile(view, profileName)
        profileAssigned = true
        cookies = WebViewCompat.getProfile(view).cookieManager.apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(view, false)
        }
        view.apply {
            isSaveEnabled = false
            filterTouchesWhenObscured = true
            if (Build.VERSION.SDK_INT >= 26) importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = false
                databaseEnabled = false
                allowFileAccess = false
                allowContentAccess = false
                @Suppress("DEPRECATION")
                allowFileAccessFromFileURLs = false
                @Suppress("DEPRECATION")
                allowUniversalAccessFromFileURLs = false
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                cacheMode = WebSettings.LOAD_NO_CACHE
                if (Build.VERSION.SDK_INT >= 26) safeBrowsingEnabled = true
            }
            setDownloadListener { _, _, _, _, _ -> abort(SourceTransferDiagnosticReason.WEB_NAVIGATION) }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                    rejectNavigation(request.url.toString())
                @Suppress("DEPRECATION")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean = rejectNavigation(url)
                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    if (rejectNavigation(url)) view.stopLoading()
                }
                override fun shouldInterceptRequest(view: WebView, resource: WebResourceRequest): WebResourceResponse? {
                    if (!closed.get() && SourceTransferWebPolicy.isResourceAllowed(resource.url.toString())) return null
                    return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                }
                override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                    handler.cancel()
                    abort(SourceTransferDiagnosticReason.WEB_TLS)
                }
                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
                    if (request.isForMainFrame) abort(SourceTransferDiagnosticReason.WEB_LOAD)
                }
                override fun onPageFinished(view: WebView, url: String) {
                    if (rejectNavigation(url)) return
                    try {
                        request.checkCurrent()
                        val checkpoint = SourceTransferWebPolicy.checkpoint(cookies?.getCookie(url)) ?: return
                        // Request profile data deletion before releasing the in-memory checkpoint.
                        close()
                        request.complete(checkpoint)
                        onCheckpoint()
                    } catch (_: Exception) { abort(SourceTransferDiagnosticReason.WEB_LOAD) }
                }
            }
        }
        layout.addView(view, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        scope.launch {
            try {
                val authenticated = withContext(Dispatchers.IO) {
                    request.checkCurrent()
                    val service = "weblogin:continue=" + URLEncoder.encode(request.fallbackUrl, "UTF-8")
                    // No foreground error resolver: it could serialize sensitive material into extras.
                    AuthManager(activity.applicationContext, request.authorization.account.name, GMS_PACKAGE_NAME, service)
                        .requestAuth(false).auth
                }
                request.checkCurrent()
                check(!closed.get())
                if (authenticated == null || !SourceTransferWebPolicy.isNavigationAllowed(authenticated)) {
                    abort(SourceTransferDiagnosticReason.WEB_AUTH_URL)
                    return@launch
                }
                view.loadUrl(authenticated)
            } catch (_: Exception) { if (!closed.get()) abort(SourceTransferDiagnosticReason.WEB_AUTHENTICATION) }
        }
    }

    private fun rejectNavigation(url: String): Boolean {
        if (closed.get()) return true
        return if (SourceTransferWebPolicy.isNavigationAllowed(url)) false
        else { abort(SourceTransferDiagnosticReason.WEB_NAVIGATION); true }
    }

    private fun abort(reason: SourceTransferDiagnosticReason) {
        request.fail(reason)
        close()
        onCheckpoint()
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        scope.cancel()
        // Removal is asynchronous; flush once it has completed so no session cookie stays on disk
        // if the process dies before the next transfer retires this profile.
        cookies?.let { manager -> manager.removeAllCookies { manager.flush() }; manager.flush() }
        cookies = null
        webView?.let {
            if (profileAssigned) WebViewCompat.getProfile(it).webStorage.deleteAllData()
            it.stopLoading()
            it.clearHistory()
            it.clearCache(true)
            (it.parent as? ViewGroup)?.removeView(it)
            it.destroy()
        }
        webView = null
        // Loaded profiles cannot be deleted by some providers until process restart. Cookies
        // are removed above; no profile is reused, and leftovers are retired before future use.
        runCatching { ProfileStore.getInstance().deleteProfile(profileName) }
    }

    companion object {
        private val providerLock = Any()
        private var processStorageConfigured = false

        private fun ensureProcessStorage() = synchronized(providerLock) {
            if (processStorageConfigured) return@synchronized
            // The consent/service run in :persistent; ordinary account WebViews run in
            // the app's main process. Even named profiles require distinct process roots.
            if (Build.VERSION.SDK_INT < 28 || Application.getProcessName() != "$GMS_PACKAGE_NAME:persistent")
                throw SourceTransferProtocolException(10575, SourceTransferDiagnosticReason.WEB_ISOLATION_UNAVAILABLE)
            try {
                // Must precede feature checks, ProfileStore and every other WebView API.
                WebView.setDataDirectorySuffix("direct_transfer")
            } catch (_: Exception) {
                throw SourceTransferProtocolException(10575, SourceTransferDiagnosticReason.WEB_ISOLATION_UNAVAILABLE)
            }
            processStorageConfigured = true
        }
    }
}
