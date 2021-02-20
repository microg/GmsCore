/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.core.ui

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.*
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import org.microg.gms.nearby.exposurenotification.ExposureScanSummary
import org.microg.gms.ui.resolveColor
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min


class DotChartView : View {
    @TargetApi(21)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context)

    var data: Set<ExposureScanSummary>? = null
        @SuppressLint("SimpleDateFormat")
        set(value) {
            field = value
            val displayData = hashMapOf<Int, Pair<String, MutableMap<Int, Int>>>()
            val now = System.currentTimeMillis()
            val min = now - 14 * 24 * 60 * 60 * 1000L
            val date = Date(min)
            val format = Settings.System.getString(context.contentResolver, Settings.System.DATE_FORMAT);
            val dateFormat = if (TextUtils.isEmpty(format)) {
                android.text.format.DateFormat.getMediumDateFormat(context)
            } else {
                SimpleDateFormat(format)
            }
            val lowest = dateFormat.parse(dateFormat.format(date))?.time ?: date.time
            for (day in 0 until 15) {
                date.time = now - (14 - day) * 24 * 60 * 60 * 1000L
                displayData[day] = dateFormat.format(date) to hashMapOf()
            }
            if (value != null) {
                for (summary in value) {
                    val off = summary.time - lowest
                    if (off < 0) continue
                    val totalHours = (off / 1000 / 60 / 60).toInt()
                    val day = totalHours / 24
                    val hour = totalHours % 24
                    displayData[day]?.second?.set(hour, (displayData[day]?.second?.get(hour) ?: 0) + summary.rpis)
                }
            }
            for (hour in 0..((min - lowest) / 1000 / 60 / 60).toInt()) {
                displayData[0]?.second?.set(hour, displayData[0]?.second?.get(hour) ?: -1)
            }
            for (hour in ((min - lowest) / 1000 / 60 / 60).toInt() until 24) {
                displayData[14]?.second?.set(hour, displayData[14]?.second?.get(hour) ?: -1)
            }
            this.displayData = displayData
            this.displayDataList = displayData.values.map { it.second.values }.flatten().sorted()
            invalidate()
        }

    private var displayData: Map<Int, Pair<String, Map<Int, Int>>> = emptyMap()
    private var displayDataList: List<Int> = emptyList()
    private val drawPaint = Paint()
    private val drawTempRect = RectF()
    private val fontPaint = Paint()
    private val fontTempRect = Rect()

    fun Canvas.drawMyRect(x: Float, y: Float, width: Float, height: Float, color: Int) {
        drawTempRect.set(x + drawPaint.strokeWidth, y + drawPaint.strokeWidth, x + width - drawPaint.strokeWidth, y + height - drawPaint.strokeWidth)
        if (Color.alpha(color) >= 80) {
            drawPaint.style = Paint.Style.FILL_AND_STROKE
            drawPaint.color = color
            drawRoundRect(drawTempRect, 2f, 2f, drawPaint)
            drawPaint.style = Paint.Style.FILL
        } else {
            drawPaint.color = color or (80 shl 24) and (80 shl 24 or 0xffffff)
            drawPaint.style = Paint.Style.STROKE
            drawRoundRect(drawTempRect, 2f, 2f, drawPaint)
            drawPaint.style = Paint.Style.FILL
            drawPaint.color = color
            drawRoundRect(drawTempRect, 2f, 2f, drawPaint)
        }
    }

    private fun <T> List<T>.relativePosition(element: T): Double? = indexOf(element).takeIf { it >= 0 }?.toDouble()?.let { it / size.toDouble() }

    private var focusPoint: PointF? = null
    override fun onDraw(canvas: Canvas) {
        if (data == null) data = emptySet()
        val d = resources.displayMetrics.scaledDensity
        fontPaint.textSize = 10 * d
        fontPaint.isAntiAlias = true
        drawPaint.isAntiAlias = true
        drawPaint.strokeWidth = 2f
        val innerPadding = 2 * d
        var maxTextWidth = 0
        var maxTextHeight = 0
        for (dateString in displayData.values.map { it.first }) {
            fontPaint.getTextBounds(dateString, 0, dateString.length, fontTempRect)
            maxTextWidth = max(maxTextWidth, fontTempRect.width())
            maxTextHeight = max(maxTextHeight, fontTempRect.height())
        }

        val legendLeft = maxTextWidth + 4 * d
        val legendBottom = maxTextHeight + 4 * d
        val subHeight = maxTextHeight + 4 * d + paddingBottom

        val distHeight = (height - innerPadding * 14 - paddingTop - paddingBottom - legendBottom - subHeight).toDouble()
        val distWidth = (width - innerPadding * 23 - paddingLeft - paddingRight - legendLeft).toDouble()
        val perHeight = distHeight / 15.0
        val perWidth = distWidth / 24.0

        val maxValue = displayDataList.last()
        val accentColor = context.resolveColor(androidx.appcompat.R.attr.colorAccent) ?: 0
        val fontColor = context.resolveColor(android.R.attr.textColorSecondary) ?: 0
        val grayBoxColor = fontColor or (255 shl 24) and (80 shl 24 or 0xffffff)
        fontPaint.textAlign = Paint.Align.RIGHT
        fontPaint.color = fontColor
        var focusDay = -1
        var focusHour = -1
        for (day in 0 until 15) {
            val (dateString, hours) = displayData[day] ?: "" to emptyMap()
            val top = day * (perHeight + innerPadding) + paddingTop
            if (day % 2 == 0) {
                canvas.drawText(dateString, (paddingLeft + legendLeft - 4 * d), (top + perHeight / 2.0 + maxTextHeight / 2.0).toFloat(), fontPaint)
            }
            focusPoint?.let { if (it.y > top && it.y < top + perHeight) focusDay = day }
            for (hour in 0 until 24) {
                val value = hours[hour]
                val left = hour * (perWidth + innerPadding) + paddingLeft + legendLeft
                when {
                    value == null -> {
                        canvas.drawMyRect(left.toFloat(), top.toFloat(), perWidth.toFloat(), perHeight.toFloat(), grayBoxColor)
                    }
                    maxValue == 0 -> {
                        canvas.drawMyRect(left.toFloat(), top.toFloat(), perWidth.toFloat(), perHeight.toFloat(), accentColor and 0xffffff)
                    }
                    value >= 0 -> {
                        val byBucket = ((displayDataList.relativePosition(value) ?: 0.0) * 180.0).toInt()
                        val byMax = (value.toDouble() / maxValue.toDouble() * 80.0).toInt()
                        val alpha = max(0, min(byBucket + byMax, 255))
                        canvas.drawMyRect(left.toFloat(), top.toFloat(), perWidth.toFloat(), perHeight.toFloat(), accentColor and (alpha shl 24 or 0xffffff))
                    }
                }
                if (focusDay == day && (value == null || value >= 0)) focusPoint?.let {
                    if (it.x > left && it.x < left + perWidth) {
                        focusHour = hour
                        canvas.drawMyRect(left.toFloat(), top.toFloat(), perWidth.toFloat(), perHeight.toFloat(), accentColor and 0xffffff)
                    }
                }
            }
        }
        val legendTop = 15 * (perHeight + innerPadding) + paddingTop + maxTextHeight + 4 * d
        fontPaint.textAlign = Paint.Align.CENTER
        for (hour in 0 until 24) {
            if (hour % 3 == 0) {
                val left = hour * (perWidth + innerPadding) + paddingLeft + legendLeft + perWidth / 2.0
                canvas.drawText("${hour}:00", left.toFloat(), legendTop.toFloat(), fontPaint)
            }
        }

        val subTop = legendTop + paddingBottom
        val subLeft = paddingLeft + legendLeft

        canvas.drawMyRect(subLeft, subTop.toFloat(), perWidth.toFloat(), perHeight.toFloat(), grayBoxColor)

        val strNoRecords = context.getString(R.string.pref_exposure_rpis_histogram_legend_no_records)
        fontPaint.textAlign = Paint.Align.LEFT
        fontPaint.getTextBounds(strNoRecords, 0, strNoRecords.length, fontTempRect)
        canvas.drawText(strNoRecords, (subLeft + perWidth + 4 * d).toFloat(), (subTop + perHeight / 2.0 + fontTempRect.height() / 2.0).toFloat(), fontPaint)

        if (maxValue >= 0) {
            canvas.drawMyRect((subLeft + (perWidth + innerPadding) * 1 + 12 * d + fontTempRect.width()).toFloat(), subTop.toFloat(), perWidth.toFloat(), perHeight.toFloat(), accentColor and 0xffffff)
            canvas.drawMyRect((subLeft + (perWidth + innerPadding) * 2 + 12 * d + fontTempRect.width()).toFloat(), subTop.toFloat(), perWidth.toFloat(), perHeight.toFloat(), accentColor and (80 shl 24 or 0xffffff))
            canvas.drawMyRect((subLeft + (perWidth + innerPadding) * 3 + 12 * d + fontTempRect.width()).toFloat(), subTop.toFloat(), perWidth.toFloat(), perHeight.toFloat(), accentColor and (170 shl 24 or 0xffffff))
            canvas.drawMyRect((subLeft + (perWidth + innerPadding) * 4 + 12 * d + fontTempRect.width()).toFloat(), subTop.toFloat(), perWidth.toFloat(), perHeight.toFloat(), accentColor)

            val strRecords = context.getString(R.string.pref_exposure_rpis_histogram_legend_records, "0 - $maxValue")
            canvas.drawText(strRecords, (subLeft + (perWidth + innerPadding) * 4 + 16 * d + fontTempRect.width() + perWidth).toFloat(), (subTop + perHeight / 2.0 + fontTempRect.height() / 2.0).toFloat(), fontPaint)
        }

        if (focusHour != -1 && Build.VERSION.SDK_INT >= 23) {
            val floatingColor = context.resolveColor(androidx.appcompat.R.attr.colorBackgroundFloating) ?: 0
            val line1 = "${displayData[focusDay]?.first}, $focusHour:00"
            val line2 = displayData[focusDay]?.second?.get(focusHour)?.let { context.getString(R.string.pref_exposure_rpis_histogram_legend_records, it.toString()) }
                    ?: strNoRecords
            fontPaint.textSize = 14 * d
            fontPaint.isFakeBoldText = true
            fontPaint.getTextBounds(line1, 0, line1.length, fontTempRect)
            var fontWidth = fontTempRect.width()
            val line1Height = fontTempRect.height() + innerPadding
            fontPaint.isFakeBoldText = false
            fontPaint.getTextBounds(line2, 0, line2.length, fontTempRect)
            fontWidth = max(fontWidth, fontTempRect.width())
            val totalHeight = line1Height + innerPadding + fontTempRect.height()
            drawPaint.color = floatingColor
            drawPaint.style = Paint.Style.FILL_AND_STROKE
            val refTop = focusDay * (perHeight + innerPadding) + paddingTop + perHeight / 2
            val refLeft = focusHour * (perWidth + innerPadding) + paddingLeft + legendLeft
            if (refLeft - fontWidth < 50 * d) {
                // To the right
                drawTempRect.set((refLeft + perWidth + innerPadding).toFloat(), (refTop - innerPadding - 5 * d - totalHeight / 2f).toFloat(), (refLeft + perWidth + innerPadding + 10 * d + fontWidth).toFloat(), (refTop + innerPadding + 5 * d + totalHeight / 2f).toFloat())
            } else {
                // To the left
                drawTempRect.set((refLeft - innerPadding - 10 * d - fontWidth).toFloat(), (refTop - innerPadding - 5 * d - totalHeight / 2f).toFloat(), (refLeft - innerPadding).toFloat(), (refTop + innerPadding + 5 * d + totalHeight / 2f).toFloat())
            }
            canvas.drawRoundRect(drawTempRect, drawPaint.strokeWidth, drawPaint.strokeWidth, drawPaint)
            val path = Path()
            if (refLeft - fontWidth < 50 * d) {
                // To the right
                val off = refLeft + perWidth + innerPadding + drawPaint.strokeWidth
                val corr = (perWidth / 2 - innerPadding + drawPaint.strokeWidth)
                path.moveTo(off.toFloat(), (refTop - corr).toFloat())
                path.lineTo((refLeft + perWidth / 2).toFloat(), refTop.toFloat())
                path.lineTo(off.toFloat(), (refTop + corr).toFloat())
            } else {
                // To the left
                val off = refLeft - innerPadding - drawPaint.strokeWidth
                val corr = (perWidth / 2 - innerPadding + drawPaint.strokeWidth)
                path.moveTo(off.toFloat(), (refTop - corr).toFloat())
                path.lineTo((refLeft + perWidth / 2).toFloat(), refTop.toFloat())
                path.lineTo(off.toFloat(), (refTop + corr).toFloat())
            }
            drawPaint.style = Paint.Style.STROKE
            drawPaint.color = accentColor and (130 shl 24 or 0xffffff)
            canvas.drawRoundRect(drawTempRect, drawPaint.strokeWidth, drawPaint.strokeWidth, drawPaint)

            drawPaint.color = floatingColor
            drawPaint.style = Paint.Style.FILL_AND_STROKE
            path.fillType = Path.FillType.EVEN_ODD
            canvas.drawPath(path, drawPaint)
            drawPaint.style = Paint.Style.STROKE
            drawPaint.color = accentColor and (130 shl 24 or 0xffffff)
            path.fillType = Path.FillType.EVEN_ODD
            canvas.drawPath(path, drawPaint)

            canvas.drawText(line2, drawTempRect.left + 5 * d, drawTempRect.top + 5 * d + totalHeight, fontPaint)
            fontPaint.isFakeBoldText = true
            canvas.drawText(line1, drawTempRect.left + 5 * d, drawTempRect.top + 5 * d + line1Height, fontPaint)
            fontPaint.isFakeBoldText = false
        }
    }

    private val removeFocusPoint = Runnable { unfocus() }

    fun unfocus() {
        focusPoint = null
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                postDelayed(removeFocusPoint, POPUP_DELAY)
                return true
            }
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_MOVE -> {
                removeCallbacks(removeFocusPoint)
                focusPoint = PointF(event.x, event.y)
                invalidate()
                return true
            }
        }
        return false
    }

    companion object {
        const val POPUP_DELAY = 3000L
    }
}
