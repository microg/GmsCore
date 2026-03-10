package org.microg.gms.maps.mapbox

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Dot
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.PatternItem
import kotlin.math.max

fun PatternItem.getName(): String = when (this) {
    is Dash -> "dash${this.length}"
    is Gap -> "gap${this.length}"
    is Dot -> "dot"
    else -> this.javaClass.name
}

/**
 * Name of pattern, to identify it after it is added to map
 */
fun List<PatternItem>.getName(color: Int, strokeWidth: Float, skew: Float = 1f) = if (isEmpty()) {
    "solid-${color}"
} else {joinToString("-") {
        it.getName()
    } + "-${color}-width${strokeWidth}-skew${skew}"
}

/**
 * Gets width that a bitmap for this pattern item would have if the pattern's bitmap
 * were to be drawn with respect to aspect ratio onto a canvas with height 1.
 */
fun PatternItem.getWidth(strokeWidth: Float, skew: Float): Float = when (this) {
    is Dash -> this.length
    is Gap -> this.length
    is Dot -> strokeWidth * skew
    else -> 1f
}

/**
 * Gets width that a bitmap for this pattern would have if it were to be drawn
 * with respect to aspect ratio onto a canvas with height 1.
 */
fun List<PatternItem>.getWidth(strokeWidth: Float, skew: Float) = map { it.getWidth(strokeWidth, skew) }.sum()

fun List<PatternItem>.makeBitmap(color: Int, strokeWidth: Float, skew: Float = 1f): Bitmap = makeBitmap(Paint().apply {
    setColor(color)
    style = Paint.Style.FILL
}, strokeWidth, skew)


fun List<PatternItem>.makeBitmap(paint: Paint, strokeWidth: Float, skew: Float): Bitmap {

    // Pattern aspect ratio is not respected by renderer
    val width = getWidth(strokeWidth, skew).toInt()
    val height = (strokeWidth * skew).toInt() // avoids squished image bugs

    // For empty list or nonsensical input (zero-width items)
    if (width == 0 || height == 0) {
        val nonZeroHeight = max(1f, strokeWidth)
        return Bitmap.createBitmap(1, nonZeroHeight.toInt(), Bitmap.Config.ARGB_8888).also {
            Canvas(it).drawRect(0f, 0f, nonZeroHeight, nonZeroHeight, paint)
        }
    }

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    var drawCursor = 0f
    for (item in this) {
        val rect = RectF(
            drawCursor,
            0f,
            drawCursor + item.getWidth(strokeWidth, skew),
            strokeWidth * skew
        )
        when (item) {
            is Dash -> canvas.drawRect(rect, paint)
            // is Gap -> do nothing, only move cursor
            is Dot -> canvas.drawOval(rect, paint)
        }

        drawCursor += item.getWidth(strokeWidth, skew)
    }

    return bitmap
}
