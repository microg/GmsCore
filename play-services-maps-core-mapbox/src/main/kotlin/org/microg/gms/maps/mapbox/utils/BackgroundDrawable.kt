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

import android.graphics.*
import android.graphics.drawable.Drawable

class BackgroundDrawable internal constructor(private val mRect: RectF,
                                              private val mCornersRadius: Float, backgroundColor: Int, strokeWidth: Float,
                                              strokeColor: Int) : Drawable() {
    private val mPaint = Paint(1)
    private val mStrokeWidth: Float
    private var mStrokePaint: Paint? = null
    private var mStrokePath: Path? = null
    private val mPath = Path()
    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
    }

    override fun draw(canvas: Canvas) {
        if (mStrokeWidth > 0.0f) {
            canvas.drawPath(mStrokePath!!, mStrokePaint!!)
        }
        canvas.drawPath(mPath, mPaint)
    }

    override fun setAlpha(alpha: Int) {
        mPaint.alpha = alpha
    }

    override fun setColorFilter(cf: ColorFilter?) {
        mPaint.colorFilter = cf
    }

    override fun getOpacity(): Int {
        return PixelFormat.OPAQUE
    }

    override fun getIntrinsicWidth(): Int {
        return mRect.width().toInt()
    }

    override fun getIntrinsicHeight(): Int {
        return mRect.height().toInt()
    }

    private fun initPath(path: Path, mStrokeWidth: Float) {
        if (mCornersRadius <= 0.0f) {
            initSquarePath(mRect, path, mStrokeWidth)
        }
        if (mStrokeWidth > 0.0f && mStrokeWidth > mCornersRadius) {
            initSquarePath(mRect, path, mStrokeWidth)
        }
        initRoundedPath(mRect, path, mStrokeWidth)
    }

    private fun initSquarePath(rect: RectF, path: Path, mStrokeWidth: Float) {
        path.moveTo(rect.left + mStrokeWidth, rect.top + mStrokeWidth)
        path.lineTo(rect.right - mStrokeWidth, rect.top + mStrokeWidth)
        path.lineTo(rect.right - mStrokeWidth, rect.bottom - mStrokeWidth)
        path.lineTo(rect.left + mStrokeWidth, rect.bottom - mStrokeWidth)
        path.lineTo(rect.left + mStrokeWidth, rect.top + mStrokeWidth)
        path.close()
    }

    private fun initRoundedPath(rect: RectF, path: Path, mStrokeWidth: Float) {
        path.moveTo(rect.left + mCornersRadius + mStrokeWidth, rect.top + mStrokeWidth)
        path.lineTo(rect.width() - mCornersRadius - mStrokeWidth, rect.top + mStrokeWidth)
        path.arcTo(RectF(rect.right - mCornersRadius, rect.top + mStrokeWidth, rect.right - mStrokeWidth, mCornersRadius + rect.top), 270.0f, 90.0f)
        path.lineTo(rect.right - mStrokeWidth, rect.bottom - mCornersRadius - mStrokeWidth)
        path.arcTo(RectF(rect.right - mCornersRadius, rect.bottom - mCornersRadius, rect.right - mStrokeWidth, rect.bottom - mStrokeWidth), 0.0f, 90.0f)
        path.lineTo(rect.left + mCornersRadius + mStrokeWidth, rect.bottom - mStrokeWidth)
        path.arcTo(RectF(rect.left + mStrokeWidth, rect.bottom - mCornersRadius, mCornersRadius + rect.left, rect.bottom - mStrokeWidth), 90.0f, 90.0f)
        path.lineTo(rect.left + mStrokeWidth, rect.top + mCornersRadius + mStrokeWidth)
        path.arcTo(RectF(rect.left + mStrokeWidth, rect.top + mStrokeWidth, mCornersRadius + rect.left, mCornersRadius + rect.top), 180.0f, 90.0f)
        path.close()
    }

    init {
        mPaint.color = backgroundColor
        mStrokeWidth = strokeWidth
        if (mStrokeWidth > 0.0f) {
            mStrokePaint = Paint(1)
            mStrokePaint!!.setColor(strokeColor)
            mStrokePath = Path()
            initPath(mPath, mStrokeWidth)
            initPath(mStrokePath!!, 0.0f)
        } else {
            initPath(mPath, 0.0f)
        }
    }
}