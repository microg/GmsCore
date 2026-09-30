/**
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.document

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.min

private const val TAG = "DocumentCaptureView"
private const val MAX_MISSED_FRAMES = 5
private const val SMOOTHING = 0.5f

@RequiresApi(21)
class DocumentCaptureView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var imageCapture: ImageCapture? = null
    private val previewView: PreviewView = PreviewView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        scaleType = PreviewView.ScaleType.FIT_CENTER
    }
    private val overlay = EdgeOverlayView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    init {
        addView(previewView)
        addView(overlay)
    }

    fun startCamera(lifecycleOwner: LifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val aspectRatio = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .build()
            val preview = Preview.Builder()
                .setResolutionSelector(aspectRatio)
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val capture = ImageCapture.Builder()
                .setResolutionSelector(aspectRatio)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setTargetRotation(display?.rotation ?: Surface.ROTATION_0)
                .build()
            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                        .setResolutionStrategy(ResolutionStrategy(Size(640, 480), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER))
                        .build()
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(analysisExecutor, ::analyze) }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture, analysis)
                imageCapture = capture
            } catch (e: Exception) {
                Log.w(TAG, "Failed to bind camera", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun analyze(image: ImageProxy) {
        try {
            val rotation = image.imageInfo.rotationDegrees
            val plane = image.planes[0]
            val corners = detectLiveCorners(plane.buffer, plane.rowStride, image.width, image.height, rotation)
            val aspectRatio = if (rotation % 180 == 0) image.width.toFloat() / image.height else image.height.toFloat() / image.width
            post { overlay.update(corners, aspectRatio) }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to analyze frame", e)
        } finally {
            image.close()
        }
    }

    fun capture(file: File, onResult: (Boolean) -> Unit) {
        val capture = imageCapture ?: return onResult(false)
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        capture.takePicture(options, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onResult(true)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.w(TAG, "Failed to capture image", exception)
                onResult(false)
            }
        })
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        analysisExecutor.shutdown()
    }
}

private class EdgeOverlayView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF4285F4.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3 * density
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x334285F4 }
    private val path = Path()
    private var corners: FloatArray? = null
    private var aspectRatio = 0f
    private var missedFrames = 0

    fun update(detected: FloatArray?, frameAspectRatio: Float) {
        aspectRatio = frameAspectRatio
        val current = corners
        if (detected == null) {
            if (++missedFrames > MAX_MISSED_FRAMES) corners = null
        } else {
            missedFrames = 0
            corners = if (current == null) detected else FloatArray(8) { current[it] + (detected[it] - current[it]) * SMOOTHING }
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val points = corners ?: return
        if (aspectRatio <= 0f || width == 0 || height == 0) return
        val frameWidth = min(width.toFloat(), height * aspectRatio)
        val frameHeight = frameWidth / aspectRatio
        val left = (width - frameWidth) / 2
        val top = (height - frameHeight) / 2
        path.reset()
        for (i in 0 until 4) {
            val x = left + points[i * 2] * frameWidth
            val y = top + points[i * 2 + 1] * frameHeight
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, linePaint)
    }
}
