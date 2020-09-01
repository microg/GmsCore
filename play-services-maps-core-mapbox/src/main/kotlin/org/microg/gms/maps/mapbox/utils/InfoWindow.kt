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

import android.annotation.SuppressLint
import android.graphics.PointF
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import com.google.android.gms.maps.model.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import org.microg.gms.maps.mapbox.R
import org.microg.gms.maps.mapbox.model.MarkerImpl

class InfoWindow(mapView: MapView, layoutResId: Int, mapboxMap: MapboxMap?) {
    private val mContentUpdateListener: ViewTreeObserver.OnGlobalLayoutListener
    var boundMarker: MarkerImpl? = null
        private set
    private var mMapboxMap: MapboxMap? = null
    protected var mView: View? = null
    private var mMarkerWidthOffset = 0f
    private var mMarkerHeightOffset = 0f
    private val mViewWidthOffset = 0f
    private var mViewHeightOffset = 0f
    private var mCoordinates: PointF? = null
    private var isVisible = false
    private val mLayoutRes: Int
    private fun initialize(view: View?, mapboxMap: MapboxMap?) {
        mMapboxMap = mapboxMap
        isVisible = false
        mView = view
    }

    fun open(mapView: MapView, boundMarker: MarkerImpl?, position: LatLng?, offsetX: Int, offsetY: Int): InfoWindow {
        setBoundMarker(boundMarker)
        if (mView != null && mMapboxMap != null) {
            mMarkerHeightOffset = offsetY.toFloat()
            mMarkerWidthOffset = -offsetX.toFloat()
            close()
            mapView.addView(mView)
            updateMarkerPosition()
            isVisible = true
        }
        return this
    }

    fun close(): InfoWindow {
        if (isVisible && mMapboxMap != null) {
            isVisible = false
            if (mView != null && mView!!.parent != null) (mView!!.parent as ViewGroup).removeView(mView)
            setBoundMarker(null)
        }
        return this
    }

    val view: View?
        get() = if (mView != null) mView else null

    fun updateMarkerText(markerItem: MarkerImpl, mapboxMap: MapboxMap?, mapView: MapView) {
        mMapboxMap = mapboxMap
        if (mView == null) {
            mView = LayoutInflater.from(mapView.context).inflate(mLayoutRes, mapView, false)
            initialize(mView, mMapboxMap)
        }
        val title = markerItem.title
        val titleTextView = mView!!.findViewById<TextView>(R.id.show_info_title)
        if (!TextUtils.isEmpty(title)) {
            titleTextView.text = title
            titleTextView.visibility = View.VISIBLE
        } else {
            titleTextView.visibility = View.GONE
        }
        val snippet = markerItem.snippet
        val snippetTextView = mView!!.findViewById<TextView>(R.id.show_info_snippet)
        if (!TextUtils.isEmpty(snippet)) {
            snippetTextView.text = snippet
            snippetTextView.visibility = View.VISIBLE
        } else {
            snippetTextView.visibility = View.GONE
        }
    }

    fun setBoundMarker(boundMarker: MarkerImpl?): InfoWindow {
        this.boundMarker = boundMarker
        return this
    }

    fun updateMarkerPosition() {
        if (mMapboxMap != null && boundMarker != null && mView != null) {
            val mbLatLng = com.mapbox.mapboxsdk.geometry.LatLng()
            mbLatLng.latitude = boundMarker!!.position.latitude
            mbLatLng.longitude = boundMarker!!.position.longitude
            mCoordinates = mMapboxMap!!.projection.toScreenLocation(mbLatLng)
            mView!!.x = mCoordinates!!.x - mView!!.measuredWidth / 2.0f + mMarkerWidthOffset
            mView!!.y = mCoordinates!!.y - mView!!.measuredHeight * 1.2f + mViewHeightOffset
        }
    }

    init {
        mContentUpdateListener = object : ViewTreeObserver.OnGlobalLayoutListener {
            @SuppressLint("NewApi")
            override fun onGlobalLayout() {
                if (mView != null) {
                    mView!!.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    mViewHeightOffset = -mView!!.measuredHeight + mMarkerHeightOffset
                    updateMarkerPosition()
                }
            }
        }
        mLayoutRes = layoutResId
        val view = LayoutInflater.from(mapView.context).inflate(layoutResId, mapView as ViewGroup, false)
        initialize(view, mapboxMap)
    }
}