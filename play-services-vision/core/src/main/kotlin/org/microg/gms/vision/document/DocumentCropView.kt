/**
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.document

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.io.File
import kotlin.math.hypot
import kotlin.math.min

private const val PREVIEW_SIZE = 1280
private const val LOUPE_ZOOM = 2.5f

class DocumentCropView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val handleRadius = 12 * density
    private val touchRadius = 36 * density
    private val loupeRadius = 56 * density
    private val loupeMargin = 8 * density
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF4285F4.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3 * density
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x334285F4 }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val loupeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2 * density
    }
    private val loupeBackgroundPaint = Paint().apply { color = Color.BLACK }
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val imageRect = RectF()
    private val path = Path()
    private val loupeClip = Path()

    private var preview: Bitmap? = null
    private var imageWidth = 0
    private var imageHeight = 0
    private var activeCorner = -1
    private var detected: FloatArray? = null

    var corners = FloatArray(8)
        private set

    val detectedCorners: FloatArray?
        get() = detected?.copyOf()

    fun setPage(file: File, detected: FloatArray?, current: FloatArray? = null) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        imageWidth = bounds.outWidth
        imageHeight = bounds.outHeight
        preview?.recycle()
        preview = decodeScaled({ file.inputStream() }, PREVIEW_SIZE)
        this.detected = detected?.copyOf()
        corners = current?.copyOf() ?: detected?.copyOf() ?: fullPageCorners(imageWidth, imageHeight)
        updateImageRect()
        invalidate()
    }

    fun clear() {
        preview?.recycle()
        preview = null
        invalidate()
    }

    fun selectDetected() {
        corners = detected?.copyOf() ?: fullPageCorners(imageWidth, imageHeight)
        invalidate()
    }

    fun selectFullPage() {
        corners = fullPageCorners(imageWidth, imageHeight)
        invalidate()
    }

    fun rotatedClockwise(): Pair<FloatArray, FloatArray?> = rotateClockwise(corners) to detected?.let { rotateClockwise(it) }

    private fun rotateClockwise(points: FloatArray): FloatArray {
        val rotated = FloatArray(8) { if (it % 2 == 0) imageHeight - points[it + 1] else points[it - 1] }
        return FloatArray(8) { rotated[(it + 6) % 8] }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateImageRect()
    }

    private fun updateImageRect() {
        if (imageWidth <= 0 || imageHeight <= 0 || width <= 0 || height <= 0) return
        val padding = touchRadius
        val scale = min((width - 2 * padding) / imageWidth, (height - 2 * padding) / imageHeight)
        val left = (width - imageWidth * scale) / 2
        val top = (height - imageHeight * scale) / 2
        imageRect.set(left, top, left + imageWidth * scale, top + imageHeight * scale)
    }

    private fun toViewX(x: Float) = imageRect.left + x / imageWidth * imageRect.width()
    private fun toViewY(y: Float) = imageRect.top + y / imageHeight * imageRect.height()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bitmap = preview ?: return
        drawPage(canvas, bitmap)
        for (i in 0 until 4) {
            val x = toViewX(corners[i * 2])
            val y = toViewY(corners[i * 2 + 1])
            canvas.drawCircle(x, y, handleRadius, handlePaint)
            canvas.drawCircle(x, y, handleRadius, linePaint)
        }
        if (activeCorner >= 0) drawLoupe(canvas, bitmap)
    }

    private fun drawPage(canvas: Canvas, bitmap: Bitmap) {
        canvas.drawBitmap(bitmap, null, imageRect, bitmapPaint)
        path.reset()
        for (i in 0 until 4) {
            val x = toViewX(corners[i * 2])
            val y = toViewY(corners[i * 2 + 1])
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, linePaint)
    }

    private fun drawLoupe(canvas: Canvas, bitmap: Bitmap) {
        val focusX = toViewX(corners[activeCorner * 2])
        val focusY = toViewY(corners[activeCorner * 2 + 1])
        val centerX = if (focusX < width / 2f) width - loupeMargin - loupeRadius else loupeMargin + loupeRadius
        val centerY = if (focusY < height / 2f) height - loupeMargin - loupeRadius else loupeMargin + loupeRadius
        loupeClip.reset()
        loupeClip.addCircle(centerX, centerY, loupeRadius, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(loupeClip)
        canvas.drawCircle(centerX, centerY, loupeRadius, loupeBackgroundPaint)
        canvas.translate(centerX, centerY)
        canvas.scale(LOUPE_ZOOM, LOUPE_ZOOM)
        canvas.translate(-focusX, -focusY)
        drawPage(canvas, bitmap)
        canvas.restore()
        canvas.drawCircle(centerX, centerY, loupeRadius, loupeBorderPaint)
        canvas.drawLine(centerX - handleRadius, centerY, centerX + handleRadius, centerY, loupeBorderPaint)
        canvas.drawLine(centerX, centerY - handleRadius, centerX, centerY + handleRadius, loupeBorderPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (preview == null) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activeCorner = (0 until 4).minByOrNull { hypot(toViewX(corners[it * 2]) - event.x, toViewY(corners[it * 2 + 1]) - event.y) }
                    ?.takeIf { hypot(toViewX(corners[it * 2]) - event.x, toViewY(corners[it * 2 + 1]) - event.y) <= touchRadius } ?: -1
                if (activeCorner >= 0) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                    invalidate()
                }
                return activeCorner >= 0
            }
            MotionEvent.ACTION_MOVE -> {
                if (activeCorner < 0) return false
                corners[activeCorner * 2] = ((event.x - imageRect.left) / imageRect.width() * imageWidth).coerceIn(0f, imageWidth.toFloat())
                corners[activeCorner * 2 + 1] = ((event.y - imageRect.top) / imageRect.height() * imageHeight).coerceIn(0f, imageHeight.toFloat())
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activeCorner = -1
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
