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
import org.microg.gms.vision.text.paddle.engine.OCREngine
import org.microg.gms.vision.text.paddle.model.OCRBox
import org.microg.gms.vision.text.paddle.model.OCRResult
import org.microg.gms.vision.text.paddle.model.RecognitionModel
import org.opencv.android.OpenCVLoader

const val TAG = "GmsTextRecognition"

class TextRecognizerHelper(
    private val modelContext: Context,
    private val recognitionModel: RecognitionModel = RecognitionModel.CJK,
) {
    private var engine: OCREngine? = null

    @Synchronized
    fun init() {
        if (engine != null) return
        check(android.os.Build.VERSION.SDK_INT >= 26) { "Offline OCR requires API 26 or newer" }
        val started = SystemClock.elapsedRealtime()
        try {
            check(OpenCVLoader.initLocal()) { "Unable to initialize OpenCV for OCR" }
            engine = OCREngine(modelContext, recognitionModel)
            Log.i(TAG, "init: ${recognitionModel.assetDirectory} ONNX Runtime CPU ready in ${SystemClock.elapsedRealtime() - started} ms")
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
        try {
            init()
            // Own the prepared pixels; never recycle or close the caller's image.
            val preparedImage = TextInputImage.prepare(source, format, width, height, rotation, region) ?: return emptyList()
            val results = try {
                engine!!.run(preparedImage.mat)
            } finally {
                preparedImage.mat.release()
            }
            val recognized = results.filter { it.text.isNotBlank() }.map { result ->
                result.copy(
                    box = OCRBox(result.box.points.map {
                        PointF(it.x * preparedImage.scaleX + preparedImage.offsetX, it.y * preparedImage.scaleY + preparedImage.offsetY)
                    }),
                    confidence = if (result.confidence.isFinite()) result.confidence.coerceIn(0f, 1f) else 0f,
                )
            }
            val characterCount = recognized.sumOf { it.text.length } + (recognized.size - 1).coerceAtLeast(0)
            // Do not put document contents in logs.
            Log.i(TAG, "process: ${recognized.size} lines, $characterCount characters, ${SystemClock.elapsedRealtime() - started} ms")
            return recognized
        } catch (e: Exception) {
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
