package org.microg.gms.maps.mapbox.model

import android.graphics.Point
import android.graphics.PointF
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.google.android.gms.dynamic.ObjectWrapper
import com.google.android.gms.dynamic.unwrap
import com.google.android.gms.maps.internal.IInfoWindowAdapter
import com.google.android.gms.maps.model.internal.IMarkerDelegate
import com.mapbox.android.gestures.Utils
import org.microg.gms.maps.mapbox.AbstractGoogleMap
import org.microg.gms.maps.mapbox.GoogleMapImpl
import org.microg.gms.maps.mapbox.R
import org.microg.gms.maps.mapbox.utils.MapContext
import org.microg.gms.maps.mapbox.utils.toMapbox
import kotlin.math.*

/**
 * `InfoWindow` is a tooltip shown when a [MarkerImpl] is tapped. Only
 * one info window is displayed at a time. When the user clicks on a marker, the currently open info
 * window will be closed and the new info window will be displayed. If the user clicks the same
 * marker while its info window is currently open, the info window will be reopened.
 *
 * The info window is drawn oriented against the device's screen, centered above its associated
 * marker, unless a different info window anchor is set. The default info window contains the title
 * in bold and snippet text below the title.
 * If neither is set, no default info window is shown.
 *
 * Based on Mapbox's / MapLibre's [com.mapbox.mapboxsdk.annotations.InfoWindow].
 *
 */

fun IInfoWindowAdapter.getInfoWindowViewFor(marker: IMarkerDelegate, mapContext: MapContext): View? {
    getInfoWindow(marker).unwrap<View?>()?.let { return it }

    getInfoContents(marker).unwrap<View>()?.let { view ->
        // Detach from previous BubbleLayout parent, if exists
        view.parent?.let { (it as ViewManager).removeView(view) }

        return FrameLayout(view.context).apply {
            ViewCompat.setBackground(this, ContextCompat.getDrawable(mapContext, R.drawable.maps_default_bubble))
            val fourDp = Utils.dpToPx(4f)
            ViewCompat.setElevation(this, fourDp)
            setPadding(fourDp.toInt(), fourDp.toInt(), fourDp.toInt(), fourDp.toInt() * 3)
            addView(view)
        }
    }

    // When a custom adapter is used, but both methods return null, the default adapter must be used
    if (this !is DefaultInfoWindowAdapter) {
        return DefaultInfoWindowAdapter(mapContext).getInfoWindowViewFor(marker, mapContext)
    }

    return null
}

class InfoWindow internal constructor(
    private val view: View, private val map: AbstractGoogleMap, internal val marker: AbstractMarker
) {
    private var coordinates: PointF = PointF(0f, 0f)
    var isVisible = false

    init {
        view.setOnClickListener {
            map.onInfoWindowClickListener?.onInfoWindowClick(marker)
        }
        view.setOnLongClickListener {
            map.onInfoWindowLongClickListener?.onInfoWindowLongClick(marker)
            true
        }
    }

    fun open(mapView: FrameLayout) {
        val layoutParams: FrameLayout.LayoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
        )
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

        close(true) // if it was already opened
        mapView.addView(view, layoutParams)
        isVisible = true

        // Set correct position
        update()
    }

    /**
     * Close this [InfoWindow] if it is visible, otherwise calling this will do nothing.
     *
     * @param silent `OnInfoWindowCloseListener` is only called if `silent` is not `false`
     */
    fun close(silent: Boolean = false) {
        if (isVisible) {
            isVisible = false
            (view.parent as ViewGroup?)?.removeView(view)
            if (!silent) {
                map.onInfoWindowCloseListener?.onInfoWindowClose(marker)
            }
        }
    }

    /**
     * Updates the position of the displayed view.
     */
    fun update() {

        if (map is GoogleMapImpl) {
            map.map?.projection?.toScreenLocation(marker.position.toMapbox())?.let {
                coordinates = it
            }
        } else {
            map.projection.toScreenLocation(marker.position)?.let {
                coordinates = PointF(it.unwrap<Point>()!!)
            }
        }

        val iconDimensions = marker.getIconDimensions()
        val width = iconDimensions?.get(0) ?: 0f
        val height = iconDimensions?.get(1) ?: 0f

        view.x =
            coordinates.x - view.measuredWidth / 2f + sin(Math.toRadians(marker.rotation.toDouble())).toFloat() * width * marker.infoWindowAnchor[0]
        view.y = coordinates.y - view.measuredHeight - max(
            height * cos(Math.toRadians(marker.rotation.toDouble())).toFloat() * marker.infoWindowAnchor[1], 0f
        )
    }
}

class DefaultInfoWindowAdapter(val context: MapContext) : IInfoWindowAdapter {
    override fun asBinder() = null

    override fun getInfoWindow(marker: IMarkerDelegate?): ObjectWrapper<View> {

        if (marker == null) return ObjectWrapper.wrap(null)

        val showDefaultMarker = (marker.title != null) || (marker.snippet != null)

        return if (!showDefaultMarker) ObjectWrapper.wrap(null)
        else ObjectWrapper.wrap(
            LayoutInflater.from(context).inflate(R.layout.maps_default_bubble_layout, null, false).apply {

                marker.title?.let {
                    val titleTextView = findViewById<TextView>(R.id.title)
                    titleTextView.text = it
                    titleTextView.visibility = VISIBLE
                }

                marker.snippet?.let {
                    val snippetTextView = findViewById<TextView>(R.id.snippet)
                    snippetTextView.text = it
                    snippetTextView.visibility = VISIBLE
                }
            }
        )
    }

    override fun getInfoContents(marker: IMarkerDelegate?) = null
}