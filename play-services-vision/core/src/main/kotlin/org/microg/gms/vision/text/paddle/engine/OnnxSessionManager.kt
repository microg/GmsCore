/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text.paddle.engine

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

private const val MAX_CACHED_INPUT_BYTES = 4 * 1024 * 1024
private const val MAX_CACHED_DETECTION_INPUT_BYTES = 1600 * 1600 * 3 * Float.SIZE_BYTES

class OnnxSessionManager(context: Context) {
    private val assets = context.assets
    private val environment = OrtEnvironment.getEnvironment()
    private var sessionOptions: OrtSession.SessionOptions? = null
    private var detectionSession: OrtSession? = null
    private var recognitionSession: OrtSession? = null
    private var orientationSession: OrtSession? = null
    private var cachedInputBuffer: FloatBuffer? = null
    private var cachedDetectionInputBuffer: WeakReference<FloatBuffer>? = null

    fun loadModels(detModelAsset: String, recModelAsset: String, orientationModelAsset: String?) {
        check(sessionOptions == null && detectionSession == null && recognitionSession == null && orientationSession == null) {
            "OCR models are already loaded"
        }
        try {
            val options = OrtSession.SessionOptions()
            sessionOptions = options
            options.setIntraOpNumThreads(minOf(4, Runtime.getRuntime().availableProcessors()))
            options.setInterOpNumThreads(1)
            options.setExecutionMode(OrtSession.SessionOptions.ExecutionMode.SEQUENTIAL)
            options.addConfigEntry("session.intra_op.allow_spinning", "0")
            detectionSession = loadModel(detModelAsset, options)
            recognitionSession = loadModel(recModelAsset, options)
            if (orientationModelAsset != null) orientationSession = loadModel(orientationModelAsset, options)
        } catch (t: Throwable) {
            try {
                release()
            } catch (cleanup: Throwable) {
                t.addSuppressed(cleanup)
            }
            throw t
        }
    }

    private fun loadModel(assetPath: String, options: OrtSession.SessionOptions): OrtSession {
        val bytes = assets.open(assetPath).use { it.readBytes() }
        require(bytes.isNotEmpty()) { "OCR model asset is empty" }
        return environment.createSession(bytes, options)
    }

    fun runDetection(tensorData: FloatArray, shape: LongArray): Pair<FloatArray, LongArray> {
        return run(checkNotNull(detectionSession) { "OCR detection model is not loaded" }, tensorData, shape, 4)
    }

    fun runRecognition(tensorData: FloatArray, shape: LongArray): Pair<FloatArray, LongArray> {
        return run(checkNotNull(recognitionSession) { "OCR recognition model is not loaded" }, tensorData, shape, 3)
    }

    fun runOrientation(tensorData: FloatArray, shape: LongArray): Pair<FloatArray, LongArray> {
        return run(checkNotNull(orientationSession) { "OCR orientation model is not loaded" }, tensorData, shape, 2)
    }

    private fun run(
        session: OrtSession,
        tensorData: FloatArray,
        shape: LongArray,
        outputRank: Int,
    ): Pair<FloatArray, LongArray> {
        require(shape.size == 4 && shape[0] == 1L && shape[1] == 3L) { "OCR input must be one NCHW image with three channels" }
        require(elementCount(shape) == tensorData.size) { "OCR input shape and data length do not match" }
        check(session.inputNames.size == 1 && session.outputNames.size == 1) { "OCR model must have one input and one output" }
        val inputBytes = tensorData.size * Float.SIZE_BYTES
        // Reuse large detector inputs while available without retaining them across GC.
        val detection = outputRank == 4
        val cached = if (detection) cachedDetectionInputBuffer?.get() else cachedInputBuffer
        val inputBuffer = cached?.takeIf { it.capacity() >= tensorData.size }
            ?: ByteBuffer.allocateDirect(inputBytes).order(ByteOrder.nativeOrder()).asFloatBuffer().also {
                if (detection) {
                    if (inputBytes <= MAX_CACHED_DETECTION_INPUT_BYTES) cachedDetectionInputBuffer = WeakReference(it)
                } else if (inputBytes <= MAX_CACHED_INPUT_BYTES) {
                    cachedInputBuffer = it
                }
            }
        inputBuffer.clear()
        inputBuffer.put(tensorData)
        inputBuffer.flip()
        OnnxTensor.createTensor(environment, inputBuffer, shape).use { input ->
            session.run(mapOf(session.inputNames.single() to input)).use { result ->
                val output = result[0] as? OnnxTensor ?: error("OCR output is not a tensor")
                val info = output.info
                check(info.type == OnnxJavaType.FLOAT) { "Invalid OCR output type" }
                val outputShape = info.shape
                check(outputShape.size == outputRank && outputShape[0] == shape[0]) { "Invalid OCR output layout" }
                if (outputRank == 4) {
                    check(outputShape[1] == 1L) { "Invalid OCR detection output layout" }
                }
                val outputSize = elementCount(outputShape)
                val outputBuffer = checkNotNull(output.floatBuffer) { "OCR output has no float buffer" }
                check(outputBuffer.remaining() == outputSize) { "OCR output tensor is truncated" }
                // getFloatBuffer() copies native data; its array outlives the result.
                val outputData = if (outputBuffer.hasArray() && outputBuffer.arrayOffset() == 0 &&
                    outputBuffer.position() == 0 && outputBuffer.array().size == outputSize
                ) {
                    outputBuffer.array()
                } else {
                    FloatArray(outputSize).also { outputBuffer.get(it) }
                }
                return outputData to outputShape
            }
        }
    }

    private fun elementCount(shape: LongArray): Int {
        var count = 1L
        val maxElements = Int.MAX_VALUE / Float.SIZE_BYTES
        for (dimension in shape) {
            require(dimension > 0 && dimension <= maxElements / count) { "Invalid or oversized OCR tensor" }
            count *= dimension
        }
        return count.toInt()
    }

    fun release() {
        val detection = detectionSession
        val recognition = recognitionSession
        val orientation = orientationSession
        val options = sessionOptions
        detectionSession = null
        recognitionSession = null
        orientationSession = null
        sessionOptions = null
        cachedInputBuffer = null
        cachedDetectionInputBuffer = null
        try {
            detection?.close()
        } finally {
            try {
                recognition?.close()
            } finally {
                try {
                    orientation?.close()
                } finally {
                    options?.close()
                }
            }
        }
    }
}
