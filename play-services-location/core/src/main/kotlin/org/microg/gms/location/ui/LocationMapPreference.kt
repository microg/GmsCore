/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.ui

import android.content.Context
import android.location.Location
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import org.microg.gms.location.core.R
import org.microg.gms.ui.resolveColor
import kotlin.math.log2

class LocationMapPreference : Preference {
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    init {
        layoutResource = R.layout.preference_full_container
    }

    var location: Location? = null
        set(value) {
            field = value
            notifyChanged()
        }

    private var mapView: View? = null
    private var circle1: Any? = null
    private var circle2: Any? = null

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.isDividerAllowedAbove = false
        holder.isDividerAllowedBelow = false
        if (location != null) {
            if (isAvailable) {
                val latLng = LatLng(location!!.latitude, location!!.longitude)
                val camera = CameraPosition.fromLatLngZoom(latLng, (21 - log2(location!!.accuracy)).coerceIn(2f, 22f))
                val container = holder.itemView as ViewGroup
                if (mapView == null) {
                    val options = GoogleMapOptions().liteMode(true).scrollGesturesEnabled(false).zoomGesturesEnabled(false).camera(camera)
                    mapView = MapView(context, options)
                    mapView?.layoutParams = FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, (height * context.resources.displayMetrics.density).toInt())
                    container.addView(mapView)
                    (mapView as MapView).onCreate(null)
                } else {
                    (mapView as MapView).getMapAsync {
                        it.moveCamera(CameraUpdateFactory.newCameraPosition(camera))
                    }
                }
                (circle1 as? Circle?)?.remove()
                (circle2 as? Circle?)?.remove()
                (mapView as MapView).getMapAsync {
                    val strokeColor = (context.resolveColor(androidx.appcompat.R.attr.colorAccent) ?: 0xff009688L.toInt())
                    val fillColor = strokeColor and 0x60ffffff
                    circle1 = it.addCircle(CircleOptions().center(latLng).radius(location!!.accuracy.toDouble()).fillColor(fillColor).strokeWidth(1f).strokeColor(strokeColor))
                    circle2 = it.addCircle(CircleOptions().center(latLng).radius(location!!.accuracy.toDouble() * 2).fillColor(fillColor).strokeWidth(1f).strokeColor(strokeColor))
                }
            } else {
                Log.d(TAG, "MapView not available")
            }
        } else if (mapView != null) {
            (mapView as MapView).onDestroy()
            (mapView?.parent as? ViewGroup?)?.removeView(mapView)
            circle1 = null
            circle2 = null
            mapView = null
        }
    }

    override fun onDetached() {
        super.onDetached()
        if (mapView != null) {
            (mapView as MapView).onDestroy()
            circle1 = null
            circle2 = null
            mapView = null
        }
    }

    companion object {
        const val height = 200f

        val isAvailable: Boolean
            get() = try {
                Class.forName("com.google.android.gms.maps.MapView")
                true
            } catch (e: ClassNotFoundException) {
                false
            }
    }
}