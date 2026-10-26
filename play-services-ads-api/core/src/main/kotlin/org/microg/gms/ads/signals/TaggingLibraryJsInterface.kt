/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.ads.signals

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.annotation.Keep
import com.google.android.gms.ads.internal.signals.SignalRequestParcel
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.ExecutionException
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

@Keep
internal class TaggingLibraryJsInterface(view: WebView, private val backend: SignalGenerationBackend) {
    private val viewRef = WeakReference(view)
    // Constructed on the WebView thread. View.post would wait indefinitely for attachment.
    private val handler = Handler(checkNotNull(Looper.myLooper()))

    @JavascriptInterface
    fun getQueryInfo(): String {
        val id = UUID.randomUUID().toString()
        SignalDiagnostics.debug("js_query", "started")
        val completed = AtomicBoolean()
        lateinit var timeout: Runnable
        fun complete(result: SignalResult) {
            if (!completed.compareAndSet(false, true)) return
            handler.removeCallbacks(timeout)
            val message = JSONObject().put("paw_id", id)
            when (result) {
                is SignalResult.Success -> message.put("signal", result.signals)
                is SignalResult.Failure -> message.put("error", result.reason.message)
            }
            SignalDiagnostics.debug("js_query_result", if (result is SignalResult.Failure) result.reason.name else "success")
            if (viewRef.get() == null) return
            handler.post {
                try { viewRef.get()?.evaluateJavascript("window.postMessage($message, '*');", null) }
                catch (_: RuntimeException) { SignalDiagnostics.debug("js_query_result", "webview_rejected") }
            }
        }
        timeout = Runnable { complete(SignalResult.Failure(SignalFailure.TIMEOUT)) }
        handler.postDelayed(timeout, MAX_WAIT_MILLIS.toLong())
        try {
            signalExecutor.execute {
                val result = try {
                    backend.query(SignalRequestParcel().apply { adFormat = "BANNER" })
                } catch (_: RuntimeException) { SignalResult.Failure(SignalFailure.INTERNAL) }
                complete(result)
            }
        } catch (_: RejectedExecutionException) { complete(SignalResult.Failure(SignalFailure.BUSY)) }
        return id
    }

    @JavascriptInterface
    fun getClickSignals(input: String?): String = signals("click", null) {
        if (input == null || input.length > MAX_INPUT_LENGTH) SignalResult.Failure(SignalFailure.INVALID_REQUEST)
        else backend.clickSignals(input)
    }

    @JavascriptInterface
    fun getViewSignals(): String = signals("view", null) { backend.viewSignals() }

    @JavascriptInterface
    fun getClickSignalsWithTimeout(input: String?, timeoutMillis: Int): String = signals("click", timeoutMillis) {
        if (input == null || input.length > MAX_INPUT_LENGTH) SignalResult.Failure(SignalFailure.INVALID_REQUEST)
        else backend.clickSignals(input)
    }

    @JavascriptInterface
    fun getViewSignalsWithTimeout(timeoutMillis: Int): String = signals("view", timeoutMillis) { backend.viewSignals() }

    private fun signals(operation: String, timeoutMillis: Int?, action: () -> SignalResult): String {
        if (timeoutMillis != null && timeoutMillis <= 0) {
            SignalDiagnostics.debug("js_$operation", "invalid_timeout")
            return ""
        }
        try {
            val result = if (timeoutMillis == null) action() else {
                val task = signalExecutor.submit<SignalResult> { action() }
                try { task.get(minOf(timeoutMillis, MAX_WAIT_MILLIS).toLong(), TimeUnit.MILLISECONDS) }
                finally { if (!task.isDone) task.cancel(true) }
            }
            return when (result) {
                is SignalResult.Success -> result.signals
                is SignalResult.Failure -> {
                    SignalDiagnostics.debug("js_$operation", result.reason.name)
                    ""
                }
            }
        } catch (_: TimeoutException) {
            SignalDiagnostics.debug("js_$operation", "timeout")
            return "17"
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            SignalDiagnostics.debug("js_$operation", "interrupted")
        } catch (_: ExecutionException) {
            SignalDiagnostics.debug("js_$operation", "backend_failed")
        } catch (_: RuntimeException) {
            SignalDiagnostics.debug("js_$operation", "backend_failed")
        }
        return ""
    }

    @JavascriptInterface
    fun recordClick(input: String?) {
        if (input.isNullOrEmpty() || input.length > MAX_INPUT_LENGTH) {
            SignalDiagnostics.debug("js_record_click", "invalid_request")
            return
        }
        try {
            signalExecutor.execute {
                try { reportResult("js_record_click", backend.recordClick(input)) }
                catch (_: RuntimeException) { SignalDiagnostics.debug("js_record_click", "backend_failed") }
            }
        } catch (_: RejectedExecutionException) { SignalDiagnostics.debug("js_record_click", "busy") }
    }

    @JavascriptInterface
    fun reportTouchEvent(input: String?) {
        if (input == null || input.length > MAX_INPUT_LENGTH) {
            SignalDiagnostics.debug("js_touch", "invalid_request")
            return
        }
        try {
            val json = JSONObject(input)
            val type = json.getInt("type")
            val duration = json.getLong("duration_ms")
            val force = json.getDouble("force").toFloat()
            if (type !in 0..3 || duration < 0 || force.isNaN() || force.isInfinite()) {
                SignalDiagnostics.debug("js_touch", "invalid_request")
                return
            }
            val event = MotionEvent.obtain(0, duration, type, json.getInt("x").toFloat(), json.getInt("y").toFloat(), force, 1f, 0, 1f, 1f, 0, 0)
            try { reportResult("js_touch", backend.reportTouchEvent(event)) } finally { event.recycle() }
        } catch (_: JSONException) { SignalDiagnostics.debug("js_touch", "invalid_json") }
        catch (_: RuntimeException) { SignalDiagnostics.debug("js_touch", "backend_failed") }
    }

    private fun reportResult(operation: String, result: SignalResult) {
        SignalDiagnostics.debug(operation, if (result is SignalResult.Failure) result.reason.name else "success")
    }

    companion object {
        private const val MAX_WAIT_MILLIS = 5000
        private const val MAX_INPUT_LENGTH = 8192
    }
}
