/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.ads.signals

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.os.RemoteException
import android.webkit.WebView
import com.google.android.gms.ads.internal.signals.ISignalCallback
import com.google.android.gms.ads.internal.signals.ISignalGenerator
import com.google.android.gms.ads.internal.signals.SignalRequestParcel
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import org.microg.gms.utils.warnOnTransactionIssues
import java.util.WeakHashMap
import java.util.concurrent.RejectedExecutionException

private const val TAG = "AdsSignalGenerator"

internal class SignalGeneratorImpl(
    private val backend: SignalGenerationBackend = UnsupportedSignalGenerationBackend
) : ISignalGenerator.Stub() {
    override fun generateSignals(context: IObjectWrapper?, request: SignalRequestParcel?, callback: ISignalCallback?) {
        SignalDiagnostics.debug("generate", if (request == null) "missing_request" else "request_received")
        if (callback == null) {
            SignalDiagnostics.debug("generate", "missing_callback")
            return
        }
        try {
            signalExecutor.execute {
                val result = try { backend.query(request) } catch (_: RuntimeException) {
                    SignalResult.Failure(SignalFailure.INTERNAL)
                }
                try {
                    when (result) {
                        is SignalResult.Success -> callback.onSuccess(result.signals, result.data, result.extras)
                        is SignalResult.Failure -> callback.onFailure(result.reason.message)
                    }
                    SignalDiagnostics.debug("generate_result", if (result is SignalResult.Failure) result.reason.name else "success")
                } catch (_: RemoteException) {
                    SignalDiagnostics.debug("generate_result", "callback_disconnected")
                } catch (_: RuntimeException) {
                    SignalDiagnostics.debug("generate_result", "callback_rejected")
                }
            }
        } catch (_: RejectedExecutionException) {
            try { callback.onFailure(SignalFailure.BUSY.message) } catch (_: RemoteException) {
                SignalDiagnostics.debug("generate_result", "callback_disconnected")
            } catch (_: RuntimeException) {
                SignalDiagnostics.debug("generate_result", "callback_rejected")
            }
            SignalDiagnostics.debug("generate_result", "busy")
        }
    }

    override fun registerWebView(webView: IObjectWrapper?) {
        val view = try { ObjectWrapper.unwrap(webView) as? WebView } catch (_: IllegalArgumentException) { null }
        if (view == null) {
            SignalDiagnostics.debug("register", "invalid_webview")
            return
        }
        val looper = if (Build.VERSION.SDK_INT >= 28) view.webViewLooper else Looper.getMainLooper()
        if (Looper.myLooper() == looper) registerOnViewThread(view)
        else {
            SignalDiagnostics.debug("register", "posted_to_webview_thread")
            if (!Handler(looper).post { registerOnViewThread(view) }) SignalDiagnostics.debug("register", "looper_unavailable")
        }
    }

    private fun registerOnViewThread(view: WebView) {
        synchronized(registeredViews) {
            if (registeredViews.containsKey(view)) {
                SignalDiagnostics.debug("register", "already_registered")
                return
            }
            val bridge = TaggingLibraryJsInterface(view, backend)
            try {
                view.addJavascriptInterface(bridge, "gmaSdk")
                registeredViews[view] = bridge
                SignalDiagnostics.debug("register", "gmaSdk_registered")
            } catch (_: RuntimeException) {
                SignalDiagnostics.debug("register", "webview_rejected")
            }
        }
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }

    companion object {
        // The bridge holds only a WeakReference to its view, so values cannot keep keys alive.
        private val registeredViews = WeakHashMap<WebView, TaggingLibraryJsInterface>()
    }
}
