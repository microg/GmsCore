/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text.paddle.engine

import org.microg.gms.vision.text.model.OCRResult
import org.microg.gms.vision.text.paddle.model.ModelConfig
import org.microg.gms.vision.text.paddle.model.OcrModelAccess
import org.microg.gms.vision.text.paddle.model.RecognitionModel
import org.microg.gms.vision.text.paddle.postprocess.BoxSorter
import org.microg.gms.vision.text.paddle.postprocess.CTCDecoder
import org.microg.gms.vision.text.paddle.postprocess.DBPostProcessor
import org.microg.gms.vision.text.paddle.postprocess.QuadTextCrop
import org.microg.gms.vision.text.paddle.postprocess.WordBoxEstimator
import org.microg.gms.vision.text.paddle.preprocess.DetPreprocessor
import org.microg.gms.vision.text.paddle.preprocess.OrientationPreprocessor
import org.microg.gms.vision.text.paddle.preprocess.RecPreprocessor
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat

private const val DETECTION_MODEL_PATH = "ocr/ppocrv6_tiny/det/inference.onnx"
private const val ORIENTATION_MODEL_PATH = "ocr/pp_lcnet_x0_25_textline_ori/inference.onnx"
private const val RECOGNITION_SCORE_THRESHOLD = 0.3f
private const val ORIENTATION_SCORE_THRESHOLD = 0.9f

internal class OCREngine(files: OcrModelAccess, recognitionModel: RecognitionModel) {
    private val sessionManager = OnnxSessionManager(files)
    private val isLatin = recognitionModel == RecognitionModel.LATIN

    // The orientation model is trained for Latin and Chinese text.
    private val useTextlineOrientation = isLatin || recognitionModel == RecognitionModel.CJK
    private val characterList = try {
        sessionManager.loadModels(
            DETECTION_MODEL_PATH,
            "${recognitionModel.assetDirectory}/inference.onnx",
            if (useTextlineOrientation) ORIENTATION_MODEL_PATH else null,
        )
        ModelConfig.loadCharacterList(files.readDictionary("${recognitionModel.assetDirectory}/inference.yml"))
    } catch (t: Throwable) {
        try {
            sessionManager.release()
        } catch (cleanup: Throwable) {
            t.addSuppressed(cleanup)
        }
        throw t
    }

    /** Reads a caller-owned BGR image without modifying or releasing it. */
    fun run(src: Mat): List<OCRResult> {
        require(!src.empty() && src.type() == CvType.CV_8UC3) { "OCR input must be a nonempty BGR image" }
        val input = DetPreprocessor.preprocess(src, sessionManager::detectionInputBuffer)
        val (output, shape) = sessionManager.runDetection(input.tensorData, input.shape)
        val boxes = DBPostProcessor.process(output, shape, src.rows(), src.cols())
        val results = mutableListOf<OCRResult>()
        var wordAnalyses = 0
        for (box in BoxSorter.sortInReadingOrder(boxes)) {
            val crop = QuadTextCrop.crop(src, box)
            try {
                val image = crop.image
                if (image.empty()) continue
                var rotated180 = false
                if (useTextlineOrientation) {
                    val orientationInput = OrientationPreprocessor.preprocess(image)
                    val (orientation, orientationShape) = sessionManager.runOrientation(orientationInput.tensorData, orientationInput.shape)
                    check(orientationShape.contentEquals(longArrayOf(1, 2)) && orientation.all { it.isFinite() && it in 0f..1f }) {
                        "Invalid OCR orientation probabilities"
                    }
                    if (orientation[1] >= ORIENTATION_SCORE_THRESHOLD && orientation[1] > orientation[0]) {
                        Core.rotate(image, image, Core.ROTATE_180)
                        rotated180 = true
                    }
                }
                val recInput = if (isLatin) RecPreprocessor.preprocessLatin(image) else RecPreprocessor.preprocess(image)
                val (recOutput, recShape) = sessionManager.runRecognition(recInput.tensorData, recInput.shape)
                val (text, confidence) = CTCDecoder.decode(recOutput, recShape, characterList)
                if (confidence >= RECOGNITION_SCORE_THRESHOLD) {
                    val words = if (text.any(Char::isWhitespace)) {
                        val analyzePixels = wordAnalyses < WordBoxEstimator.MAX_LINES_PER_IMAGE
                        if (analyzePixels) wordAnalyses++
                        WordBoxEstimator.estimate(crop, box, text, rotated180, analyzePixels)
                    } else {
                        emptyList()
                    }
                    results.add(OCRResult(box, text, confidence, words, crop.readingStartCorner(rotated180)))
                }
            } finally {
                crop.release()
            }
        }
        return results
    }

    fun release() {
        sessionManager.release()
    }
}
