/*
 * Copyright (C) 2020 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.maps.mapbox.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.LinearLayout
import org.microg.gms.maps.mapbox.R

class InfoWindowLayout @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null as AttributeSet?, defStyleAttr: Int = 0) : LinearLayout(context, attrs, defStyleAttr) {
    private var mBackgroundDrawable: BackgroundDrawable? = null
    private var cornersRadius: Float
    private var bgColor: Int
    private var strokeWidth: Float
    private var strokeColor: Int

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        initDrawable(0, width, 0, height)
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (mBackgroundDrawable != null) {
            mBackgroundDrawable!!.draw(canvas)
        }
        super.dispatchDraw(canvas)
    }

    fun setCornersRadius(cornersRadius: Float): InfoWindowLayout {
        this.cornersRadius = cornersRadius
        requestLayout()
        return this
    }

    fun setBGColor(backgroundColor: Int): InfoWindowLayout {
        this.bgColor = backgroundColor
        requestLayout()
        return this
    }

    fun setStrokeWidth(strokeWidth: Float): InfoWindowLayout {
        resetPadding()
        this.strokeWidth = strokeWidth
        initPadding()
        return this
    }

    fun setStrokeColor(strokeColor: Int): InfoWindowLayout {
        this.strokeColor = strokeColor
        requestLayout()
        return this
    }

    private fun initPadding() {
        var paddingLeft = paddingLeft
        var paddingRight = paddingRight
        var paddingTop = paddingTop
        var paddingBottom = paddingBottom
        if (strokeWidth > 0.0f) {
            paddingLeft = (paddingLeft + strokeWidth).toInt()
            paddingRight = (paddingRight + strokeWidth).toInt()
            paddingTop = (paddingTop + strokeWidth).toInt()
            paddingBottom = (paddingBottom + strokeWidth).toInt()
        }
        setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
    }

    private fun initDrawable(left: Int, right: Int, top: Int, bottom: Int) {
        if (right < left || bottom < top) {
            return
        }
        val rectF = RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
        mBackgroundDrawable = BackgroundDrawable(rectF, cornersRadius, bgColor, strokeWidth, strokeColor)
    }

    private fun resetPadding() {
        var paddingLeft = paddingLeft
        var paddingRight = paddingRight
        var paddingTop = paddingTop
        var paddingBottom = paddingBottom
        if (strokeWidth > 0.0f) {
            paddingLeft = (paddingLeft - strokeWidth).toInt()
            paddingRight = (paddingRight - strokeWidth).toInt()
            paddingTop = (paddingTop - strokeWidth).toInt()
            paddingBottom = (paddingBottom - strokeWidth).toInt()
        }
        setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
    }

    init {
        val a = getContext().obtainStyledAttributes(attrs, R.styleable.infowindowLayout)
        cornersRadius = a.getDimension(R.styleable.infowindowLayout_infowindow_corner_radius, 0.0f)
        bgColor = a.getColor(R.styleable.infowindowLayout_infowindow_bg_color, -0x1)
        strokeWidth = a.getDimension(R.styleable.infowindowLayout_infowindow_stroke_width, -1.0f)
        strokeColor = a.getColor(R.styleable.infowindowLayout_infowindow_stroke_color, -0x99999a)
        a.recycle()
        initPadding()
    }
}