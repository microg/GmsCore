/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text

import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.os.SystemClock
import android.util.Log
import org.microg.gms.vision.text.model.OCRBox
import org.microg.gms.vision.text.model.OCRResult
import org.microg.gms.vision.text.model.RecognitionModel

const val TAG = "GmsTextRecognition"

internal class OcrModelUnavailableException : IllegalStateException("OCR model is not ready")

class TextRecognizerHelper(
    private val callerContext: Context,
    private val recognitionModel: RecognitionModel = RecognitionModel.CJK,
) {
    private var engine: OcrBackend? = null

    @Synchronized
    fun init() {
        if (engine != null) return
        check(android.os.Build.VERSION.SDK_INT >= 26) { "Offline OCR requires API 26 or newer" }
        val started = SystemClock.elapsedRealtime()
        try {
            engine = OcrBackend(callerContext, recognitionModel)
            Log.i(TAG, "init: $recognitionModel OCR ready in ${SystemClock.elapsedRealtime() - started} ms")
        } catch (e: OcrModelUnavailableException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "init: failed to initialize offline OCR", e)
            throw IllegalStateException("Failed to initialize offline OCR", e)
        } catch (e: LinkageError) {
            Log.e(TAG, "init: native OCR runtime is unavailable", e)
            throw IllegalStateException("Native OCR runtime is unavailable", e)
        }
    }

    @Synchronized
    fun recognize(source: Any, format: Int, width: Int, height: Int, rotation: Int, region: Rect? = null): List<OCRResult> {
        val started = SystemClock.elapsedRealtime()
        init()
        val initialized = SystemClock.elapsedRealtime()
        var inputPrepared = false
        try {
            val preparedImage = TextInputImage.prepare(source, format, width, height, rotation, region) ?: return emptyList()
            inputPrepared = true
            val prepared = SystemClock.elapsedRealtime()
            val results = try {
                engine!!.run(preparedImage.bitmap)
            } finally {
                if (preparedImage.bitmap !== source) preparedImage.bitmap.recycle()
            }
            val inferred = SystemClock.elapsedRealtime()
            fun mapBox(box: OCRBox) = OCRBox(box.points.map {
                PointF(it.x * preparedImage.scaleX + preparedImage.offsetX, it.y * preparedImage.scaleY + preparedImage.offsetY)
            })
            val recognized = results.filter { it.text.isNotBlank() }.map { result ->
                result.copy(
                    box = mapBox(result.box),
                    confidence = if (result.confidence.isFinite()) result.confidence.coerceIn(0f, 1f) else 0f,
                    words = result.words.map { it.copy(box = mapBox(it.box)) },
                )
            }
            val characterCount = recognized.sumOf { it.text.length } + (recognized.size - 1).coerceAtLeast(0)
            val finished = SystemClock.elapsedRealtime()
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "timing: init=${initialized - started} ms, prepare=${prepared - initialized} ms, inference=${inferred - prepared} ms, map=${finished - inferred} ms")
            }
            // Do not put document contents in logs.
            Log.i(TAG, "process: ${recognized.size} lines, $characterCount characters, ${finished - started} ms")
            return recognized
        } catch (e: OcrModelUnavailableException) {
            throw e
        } catch (e: Exception) {
            if (!inputPrepared && e is IllegalArgumentException) throw e
            Log.e(TAG, "process: OCR failed (format=$format, ${width}x$height, rotation=$rotation)", e)
            throw IllegalStateException("Offline text recognition failed", e)
        } catch (e: LinkageError) {
            Log.e(TAG, "process: native OCR runtime failed", e)
            throw IllegalStateException("Native OCR runtime failed", e)
        }
    }

    @Synchronized
    fun close() {
        try {
            engine?.release()
        } finally {
            engine = null
        }
        Log.d(TAG, "close: recognizer released")
    }
}
