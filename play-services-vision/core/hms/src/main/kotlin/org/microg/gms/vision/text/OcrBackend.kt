/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.PointF
import android.util.Log
import com.huawei.hms.mlsdk.common.MLFrame
import com.huawei.hms.mlsdk.text.MLLocalTextSetting
import com.huawei.hms.mlsdk.text.MLText
import com.huawei.hms.mlsdk.text.MLTextAnalyzer
import org.microg.gms.vision.text.model.OCRBox
import org.microg.gms.vision.text.model.OCRResult
import org.microg.gms.vision.text.model.OCRWord
import org.microg.gms.vision.text.model.RecognitionModel
import java.io.IOException

internal class OcrBackend(context: Context, recognitionModel: RecognitionModel) {
    private val context = context.applicationContext ?: context
    private val language = when (recognitionModel) {
        RecognitionModel.LATIN -> null
        RecognitionModel.CJK -> "zh"
        RecognitionModel.JAPANESE -> "ja"
        RecognitionModel.KOREAN -> "ko"
    }

    fun run(bitmap: Bitmap): List<OCRResult> {
        val blocks = synchronized(analyzerLock) {
            // HMS shares one native model across analyzers in the caller process.
            if (activeBackend?.language != language || activeBackend?.context !== context) {
                stopAnalyzer()
                activeAnalyzer = checkNotNull(MLTextAnalyzer.Factory(context).apply {
                    setLocalOCRMode(MLLocalTextSetting.OCR_DETECT_MODE)
                    language?.let(::setLanguage)
                }.create()) { "Unable to create the HMS text analyzer" }
            }
            activeBackend = this
            val analyzer = checkNotNull(activeAnalyzer)
            analyzer.analyseFrame(MLFrame.fromBitmap(bitmap)).also {
                if (!analyzer.isAvailable) throw OcrModelUnavailableException()
            }
        }
        return buildList {
            for (index in 0 until blocks.size()) {
                for (line in blocks.valueAt(index).contents.orEmpty()) {
                    val text = line.stringValue?.takeIf(String::isNotBlank) ?: continue
                    val box = line.toBox(bitmap.width, bitmap.height) ?: continue
                    val words = line.contents.orEmpty().mapNotNull { word ->
                        val wordText = word.stringValue?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                        val wordBox = word.toBox(bitmap.width, bitmap.height) ?: return@mapNotNull null
                        OCRWord(wordBox, wordText)
                    }
                    // HMS confidence is only available for cloud recognition.
                    add(OCRResult(box, text, 0f, words))
                }
            }
        }
    }

    fun release() = synchronized(analyzerLock) {
        if (activeBackend === this) stopAnalyzer()
    }

    private companion object {
        val analyzerLock = Any()
        var activeBackend: OcrBackend? = null
        var activeAnalyzer: MLTextAnalyzer? = null

        fun stopAnalyzer() {
            val analyzer = activeAnalyzer
            activeAnalyzer = null
            activeBackend = null
            try {
                analyzer?.stop()
            } catch (e: IOException) {
                Log.w(TAG, "Unable to stop the HMS text analyzer", e)
            }
        }
    }
}

private fun MLText.Base.toBox(width: Int, height: Int): OCRBox? {
    val corners = vertexes?.takeIf { it.size == 4 } ?: run {
        val bounds = border?.takeUnless { it.isEmpty } ?: return null
        arrayOf(
            Point(bounds.left, bounds.top),
            Point(bounds.right, bounds.top),
            Point(bounds.right, bounds.bottom),
            Point(bounds.left, bounds.bottom),
        )
    }
    return OCRBox(corners.map {
        PointF(it.x.coerceIn(0, width).toFloat(), it.y.coerceIn(0, height).toFloat())
    })
}
