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

class DocumentCropView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val handleRadius = 12 * density
    private val touchRadius = 36 * density
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF4285F4.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3 * density
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x334285F4 }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val imageRect = RectF()
    private val path = Path()

    private var preview: Bitmap? = null
    private var imageWidth = 0
    private var imageHeight = 0
    private var activeCorner = -1

    var corners = FloatArray(8)
        private set

    fun setPage(file: File, detected: FloatArray?) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        imageWidth = bounds.outWidth
        imageHeight = bounds.outHeight
        preview?.recycle()
        preview = decodeScaled({ file.inputStream() }, PREVIEW_SIZE)
        corners = detected?.copyOf() ?: fullPageCorners(imageWidth, imageHeight)
        updateImageRect()
        invalidate()
    }

    fun clear() {
        preview?.recycle()
        preview = null
        invalidate()
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
        for (i in 0 until 4) {
            val x = toViewX(corners[i * 2])
            val y = toViewY(corners[i * 2 + 1])
            canvas.drawCircle(x, y, handleRadius, handlePaint)
            canvas.drawCircle(x, y, handleRadius, linePaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (preview == null) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activeCorner = (0 until 4).minByOrNull { hypot(toViewX(corners[it * 2]) - event.x, toViewY(corners[it * 2 + 1]) - event.y) }
                    ?.takeIf { hypot(toViewX(corners[it * 2]) - event.x, toViewY(corners[it * 2 + 1]) - event.y) <= touchRadius } ?: -1
                if (activeCorner >= 0) parent?.requestDisallowInterceptTouchEvent(true)
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
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
