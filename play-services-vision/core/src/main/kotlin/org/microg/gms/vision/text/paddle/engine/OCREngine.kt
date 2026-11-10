/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text.paddle.engine

import android.content.Context
import org.microg.gms.vision.text.paddle.model.ModelConfig
import org.microg.gms.vision.text.paddle.model.OCRResult
import org.microg.gms.vision.text.paddle.model.RecognitionModel
import org.microg.gms.vision.text.paddle.postprocess.BoxSorter
import org.microg.gms.vision.text.paddle.postprocess.CTCDecoder
import org.microg.gms.vision.text.paddle.postprocess.DBPostProcessor
import org.microg.gms.vision.text.paddle.postprocess.QuadTextCrop
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

class OCREngine(context: Context, recognitionModel: RecognitionModel) {
    private val sessionManager = OnnxSessionManager(context)

    // The orientation model is trained for Latin and Chinese text.
    private val useTextlineOrientation = recognitionModel == RecognitionModel.LATIN || recognitionModel == RecognitionModel.CJK
    private val characterList = try {
        sessionManager.loadModels(
            DETECTION_MODEL_PATH,
            "${recognitionModel.assetDirectory}/inference.onnx",
            if (useTextlineOrientation) ORIENTATION_MODEL_PATH else null,
        )
        ModelConfig.loadCharacterList(context, "${recognitionModel.assetDirectory}/inference.yml")
    } catch (t: Throwable) {
        sessionManager.release()
        throw t
    }

    /** Reads a caller-owned BGR image without modifying or releasing it. */
    fun run(src: Mat): List<OCRResult> {
        require(!src.empty() && src.type() == CvType.CV_8UC3) { "OCR input must be a nonempty BGR image" }
        val input = DetPreprocessor.preprocess(src)
        val (output, shape) = sessionManager.runDetection(input.tensorData, input.shape)
        val boxes = DBPostProcessor.process(output, shape, src.rows(), src.cols())
        val results = mutableListOf<OCRResult>()
        for (box in BoxSorter.sortInReadingOrder(boxes)) {
            val crop = QuadTextCrop.crop(src, box)
            try {
                if (crop.empty()) continue
                if (useTextlineOrientation) {
                    val orientationInput = OrientationPreprocessor.preprocess(crop)
                    val (orientation, orientationShape) = sessionManager.runOrientation(orientationInput.tensorData, orientationInput.shape)
                    check(orientationShape.contentEquals(longArrayOf(1, 2)) && orientation.all { it.isFinite() && it in 0f..1f }) {
                        "Invalid OCR orientation probabilities"
                    }
                    if (orientation[1] >= ORIENTATION_SCORE_THRESHOLD && orientation[1] > orientation[0]) {
                        Core.rotate(crop, crop, Core.ROTATE_180)
                    }
                }
                val recInput = RecPreprocessor.preprocess(crop)
                val (recOutput, recShape) = sessionManager.runRecognition(recInput.tensorData, recInput.shape)
                val (text, confidence) = CTCDecoder.decode(recOutput, recShape, characterList)
                if (confidence >= RECOGNITION_SCORE_THRESHOLD) {
                    results.add(OCRResult(box, text, confidence))
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
