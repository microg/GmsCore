/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text

import android.content.Context
import android.graphics.Rect
import android.os.SystemClock
import android.util.Log
import org.microg.gms.vision.text.model.OCRResult
import org.microg.gms.vision.text.model.mapToSource
import org.microg.gms.vision.text.paddle.engine.OCREngine
import org.microg.gms.vision.text.paddle.model.OcrModelAccess
import org.microg.gms.vision.text.paddle.model.OcrModelUnavailableException
import org.microg.gms.vision.text.paddle.model.RecognitionModel
import org.opencv.android.OpenCVLoader

const val TAG = "GmsTextRecognition"
private const val MODEL_RECHECK_DELAY_MS = 1000L

class TextRecognizerHelper(
    context: Context,
    private val recognitionModel: RecognitionModel = RecognitionModel.LATIN,
) {
    private val files = OcrModelAccess(context, recognitionModel)
    private var engine: OCREngine? = null
    private var modelReady = false
    private var nextModelCheckAt = 0L
    private var closed = false

    private fun isModelReady(): Boolean {
        if (closed) return false
        if (files.hasChanged()) {
            releaseEngine()
            modelReady = false
            nextModelCheckAt = 0L
        }
        if (modelReady) return true
        val now = SystemClock.elapsedRealtime()
        if (now < nextModelCheckAt) return false
        nextModelCheckAt = now + MODEL_RECHECK_DELAY_MS
        files.observeChanges()
        modelReady = files.request()
        return modelReady
    }

    private fun init() {
        if (!isModelReady()) throw OcrModelUnavailableException()
        if (engine != null) return
        check(android.os.Build.VERSION.SDK_INT >= 26) { "Offline OCR requires API 26 or newer" }
        val started = SystemClock.elapsedRealtime()
        try {
            check(OpenCVLoader.initLocal()) { "Unable to initialize OpenCV for OCR" }
            engine = OCREngine(files, recognitionModel)
            Log.i(TAG, "init: ${recognitionModel.assetDirectory} ONNX Runtime CPU ready in ${SystemClock.elapsedRealtime() - started} ms")
        } catch (e: OcrModelUnavailableException) {
            modelReady = false
            nextModelCheckAt = 0L
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
            // Own the prepared pixels; never recycle or close the caller's image.
            val preparedImage = PaddleTextInputImage.prepare(source, format, width, height, rotation, region) ?: return emptyList()
            inputPrepared = true
            val prepared = SystemClock.elapsedRealtime()
            val results = try {
                engine!!.run(preparedImage.mat)
            } finally {
                preparedImage.mat.release()
            }
            val inferred = SystemClock.elapsedRealtime()
            val recognized = results.mapToSource(
                preparedImage.scaleX, preparedImage.scaleY, preparedImage.offsetX, preparedImage.offsetY,
            )
            val characterCount = recognized.sumOf { it.text.length } + (recognized.size - 1).coerceAtLeast(0)
            val finished = SystemClock.elapsedRealtime()
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "timing: init=${initialized - started} ms, prepare=${prepared - initialized} ms, inference=${inferred - prepared} ms, map=${finished - inferred} ms")
            }
            // Do not put document contents in logs.
            Log.i(TAG, "process: ${recognized.size} lines, $characterCount characters, ${finished - started} ms")
            return recognized
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
            releaseEngine()
        } finally {
            closed = true
            files.close()
        }
        Log.d(TAG, "close: recognizer released")
    }

    private fun releaseEngine() {
        try {
            engine?.release()
        } finally {
            engine = null
        }
    }
}
