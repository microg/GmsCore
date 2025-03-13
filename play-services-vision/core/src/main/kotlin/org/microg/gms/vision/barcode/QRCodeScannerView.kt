/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.barcode

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.internal.Barcode
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class QRCodeScannerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var onQRCodeScanned: ((Barcode?) -> Unit)? = null
    private val previewView: PreviewView = PreviewView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    init {
        addView(previewView)
        addView(ScanOverlayView(context))
    }

    fun startScanner(onScanned: (Barcode?) -> Unit) {
        this.onQRCodeScanned = onScanned
        startCamera()
    }

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val imageAnalyzer = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also {
                it.setAnalyzer(cameraExecutor, QRCodeAnalyzer { result ->
                    post { onQRCodeScanned?.invoke(result) }
                })
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(context as androidx.lifecycle.LifecycleOwner, cameraSelector, preview, imageAnalyzer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cameraExecutor.shutdown()
    }
}

private class ScanOverlayView(context: Context) : View(context) {
    private val cornerLength = 160f
    private val cornerThickness = 10f
    private val paint = Paint().apply {
        isAntiAlias = true
        strokeWidth = cornerThickness
        style = Paint.Style.STROKE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val frameSize = width.coerceAtMost(height) * 0.6f
        val left = (width - frameSize) / 2f
        val top = (height - frameSize) / 2f
        val right = left + frameSize
        val bottom = top + frameSize
        val frame = RectF(left, top, right, bottom)

        val colors = listOf(0xFF4285F4.toInt(), 0xFFEA4335.toInt(), 0xFFFBBC05.toInt(), 0xFF34A853.toInt())

        paint.color = colors[0]
        canvas.drawLine(frame.left, frame.top, frame.left + cornerLength, frame.top, paint)
        canvas.drawLine(frame.left, frame.top, frame.left, frame.top + cornerLength, paint)

        paint.color = colors[1]
        canvas.drawLine(frame.right, frame.top, frame.right - cornerLength, frame.top, paint)
        canvas.drawLine(frame.right, frame.top, frame.right, frame.top + cornerLength, paint)

        paint.color = colors[2]
        canvas.drawLine(frame.left, frame.bottom, frame.left + cornerLength, frame.bottom, paint)
        canvas.drawLine(frame.left, frame.bottom, frame.left, frame.bottom - cornerLength, paint)

        paint.color = colors[3]
        canvas.drawLine(frame.right, frame.bottom, frame.right - cornerLength, frame.bottom, paint)
        canvas.drawLine(frame.right, frame.bottom, frame.right, frame.bottom - cornerLength, paint)
    }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private class QRCodeAnalyzer(private val onQRCodeScanned: (Barcode?) -> Unit) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
    }

    override fun analyze(image: ImageProxy) {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val source = PlanarYUVLuminanceSource(bytes, image.width, image.height, 0, 0, image.width, image.height, false)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

        try {
            val result = reader.decode(binaryBitmap)
            onQRCodeScanned(result.toMlKit())
        } catch (e: NotFoundException) {
            onQRCodeScanned(null)
        } finally {
            image.close()
        }
    }
}
