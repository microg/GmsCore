/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.core.ui

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.provider.Settings
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import org.microg.gms.nearby.exposurenotification.ExposureScanSummary
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max


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
            for (hour in 0..((min-lowest)/1000/60/60).toInt()) {
                displayData[0]?.second?.set(hour, displayData[0]?.second?.get(hour) ?: -1)
            }
            for (hour in ((min-lowest)/1000/60/60).toInt() until 24) {
                displayData[14]?.second?.set(hour, displayData[14]?.second?.get(hour) ?: -1)
            }
            this.displayData = displayData
            invalidate()
        }

    private var displayData: Map<Int, Pair<String, Map<Int, Int>>> = emptyMap()
    private val paint = Paint()
    private val tempRect = Rect()
    private val tempRectF = RectF()

    private fun fetchAccentColor(): Int {
        val typedValue = TypedValue()
        val a: TypedArray = context.obtainStyledAttributes(typedValue.data, intArrayOf(androidx.appcompat.R.attr.colorAccent))
        val color = a.getColor(0, 0)
        a.recycle()
        return color
    }

    override fun onDraw(canvas: Canvas) {
        if (data == null) data = emptySet()
        paint.textSize = 10 * resources.displayMetrics.scaledDensity
        paint.isAntiAlias = true
        paint.strokeWidth = 2f
        var maxTextWidth = 0
        var maxTextHeight = 0
        for (dateString in displayData.values.map { it.first }) {
            paint.getTextBounds(dateString, 0, dateString.length, tempRect)
            maxTextWidth = max(maxTextWidth, tempRect.width())
            maxTextHeight = max(maxTextHeight, tempRect.height())
        }

        val legendLeft = maxTextWidth + 4 * resources.displayMetrics.scaledDensity
        val legendBottom = maxTextHeight + 4 * resources.displayMetrics.scaledDensity

        val distHeight = (height - 28 - paddingTop - paddingBottom - legendBottom).toDouble()
        val distWidth = (width - 46 - paddingLeft - paddingRight - legendLeft).toDouble()
        val perHeight = distHeight / 15.0
        val perWidth = distWidth / 24.0

        paint.textAlign = Paint.Align.RIGHT
        val maxValue = displayData.values.mapNotNull { it.second.values.maxOrNull() }.maxOrNull() ?: 0
        val accentColor = fetchAccentColor()
        val accentRed = Color.red(accentColor)
        val accentGreen = Color.green(accentColor)
        val accentBlue = Color.blue(accentColor)
        for (day in 0 until 15) {
            val (dateString, hours) = displayData[day] ?: "" to emptyMap()
            val top = day * (perHeight + 2) + paddingTop
            if (day % 2 == 0) {
                paint.setARGB(255, 100, 100, 100)
                canvas.drawText(dateString, (paddingLeft + legendLeft - 4 * resources.displayMetrics.scaledDensity), (top + perHeight / 2.0 + maxTextHeight / 2.0).toFloat(), paint)
            }
            for (hour in 0 until 24) {
                val value = hours[hour] ?: 0 // TODO: Actually allow null to display offline state as soon as we properly record it
                val left = hour * (perWidth + 2) + paddingLeft + legendLeft
                tempRectF.set(left.toFloat() + 2f, top.toFloat() + 2f, (left + perWidth).toFloat() - 2f, (top + perHeight).toFloat() - 2f)
                when {
                    value == null -> {
                        paint.style = Paint.Style.FILL_AND_STROKE
                        paint.setARGB(30, 100, 100, 100)
                        canvas.drawRoundRect(tempRectF, 2f, 2f, paint)
                        paint.style = Paint.Style.FILL
                    }
                    maxValue == 0 -> {
                        paint.setARGB(50, accentRed, accentGreen, accentBlue)
                        paint.style = Paint.Style.STROKE
                        canvas.drawRoundRect(tempRectF, 2f, 2f, paint)
                        paint.style = Paint.Style.FILL
                    }
                    value >= 0 -> {
                        val alpha = ((value.toDouble() / maxValue.toDouble()) * 255).toInt()
                        paint.setARGB(max(50, alpha), accentRed, accentGreen, accentBlue)
                        paint.style = Paint.Style.STROKE
                        canvas.drawRoundRect(tempRectF, 2f, 2f, paint)
                        paint.style = Paint.Style.FILL
                        paint.setARGB(alpha, accentRed, accentGreen, accentBlue)
                        canvas.drawRoundRect(tempRectF, 2f, 2f, paint)
                    }
                }
            }
        }
        val legendTop = 15 * (perHeight + 2) + paddingTop + maxTextHeight + 4 * resources.displayMetrics.scaledDensity
        paint.textAlign = Paint.Align.CENTER
        paint.setARGB(255, 100, 100, 100)
        for (hour in 0 until 24) {
            if (hour % 3 == 0) {
                val left = hour * (perWidth + 2) + paddingLeft + legendLeft + perWidth / 2.0
                canvas.drawText("${hour}:00", left.toFloat(), legendTop.toFloat(), paint)
            }
        }
    }
}
