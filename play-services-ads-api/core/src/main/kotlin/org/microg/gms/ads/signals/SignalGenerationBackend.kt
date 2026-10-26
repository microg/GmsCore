/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.ads.signals

import android.os.Bundle
import android.view.MotionEvent
import com.google.android.gms.ads.internal.signals.SignalRequestParcel
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

internal sealed class SignalResult {
    data class Success(val signals: String, val data: String, val extras: Bundle) : SignalResult()
    data class Failure(val reason: SignalFailure) : SignalResult()
}

internal enum class SignalFailure(val message: String) {
    UNSUPPORTED("Signal generation is not supported."),
    INVALID_REQUEST("Invalid signal request."),
    TIMEOUT("Signal generation timed out."),
    BUSY("Too many pending signal requests."),
    INTERNAL("Signal generation failed.")
}

/** Explicit capability boundary. Registration works independently of Google signal generation. */
internal interface SignalGenerationBackend {
    fun query(request: SignalRequestParcel?): SignalResult
    fun clickSignals(input: String): SignalResult
    fun viewSignals(): SignalResult
    fun recordClick(input: String): SignalResult
    fun reportTouchEvent(event: MotionEvent): SignalResult
}

// No local Google signal engine exists yet. Never manufacture a successful signal or request ID.
internal object UnsupportedSignalGenerationBackend : SignalGenerationBackend {
    override fun query(request: SignalRequestParcel?): SignalResult = SignalResult.Failure(
        if (request == null) SignalFailure.INVALID_REQUEST else SignalFailure.UNSUPPORTED
    )
    override fun clickSignals(input: String): SignalResult = SignalResult.Failure(SignalFailure.UNSUPPORTED)
    override fun viewSignals(): SignalResult = SignalResult.Failure(SignalFailure.UNSUPPORTED)
    override fun recordClick(input: String): SignalResult = SignalResult.Failure(SignalFailure.UNSUPPORTED)
    override fun reportTouchEvent(event: MotionEvent): SignalResult = SignalResult.Failure(SignalFailure.UNSUPPORTED)
}

internal val signalExecutor = ThreadPoolExecutor(
    1, 2, 30, TimeUnit.SECONDS, ArrayBlockingQueue(16),
    { task -> Thread(task, "microG-AdsSignals").apply { isDaemon = true } }
).apply { allowCoreThreadTimeOut(true) }
