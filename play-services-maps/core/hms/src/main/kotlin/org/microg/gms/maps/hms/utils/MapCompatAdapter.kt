/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.hms.utils

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * Adapter that applies app-specific compatibility fixes for HMS Maps
 * based on the calling application's package name.
 */
class MapCompatAdapter(context: Context) {

    private val callerPackageName: String = context.applicationContext?.packageName ?: context.packageName

    /**
     * Wraps the mapView in a traversal-blocking container if needed.
     * Some apps traverse the view hierarchy and encounter HMS internal views,
     * causing crashes or unexpected behavior.
     *
     * @return the wrapper view to add to the root, or mapView itself if no wrapping is needed.
     */
    fun wrapMapView(mapContext: Context, mapView: View): View {
        if (!needsViewTraversalBlock().also { Log.d(TAG, "$callerPackageName need wrapMapView ? $it") }) return mapView

        val blocker = object : FrameLayout(mapContext) {
            override fun getChildCount(): Int = 0
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                val lp = mapView.layoutParams
                    ?: LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                val childW = getChildMeasureSpec(widthMeasureSpec, 0, lp.width)
                val childH = getChildMeasureSpec(heightMeasureSpec, 0, lp.height)
                mapView.measure(childW, childH)
                setMeasuredDimension(
                    resolveSize(mapView.measuredWidth, widthMeasureSpec),
                    resolveSize(mapView.measuredHeight, heightMeasureSpec)
                )
            }

            override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
                mapView.layout(0, 0, right - left, bottom - top)
            }
        }
        blocker.addView(
            mapView,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        return blocker
    }

    /**
     * Intercepts map click events in liteMode for apps that need special handling.
     * @return true if the click was intercepted and handled, false to proceed normally.
     */
    fun interceptLiteModeClick(rootView: View): Boolean {
        return findLiteModeParent(rootView)?.let {
            it.performClick()
            true
        } ?: false
    }

    /**
     * Intercepts map long-click events in liteMode for apps that need special handling.
     * @return true if the long-click was intercepted and handled, false to proceed normally.
     */
    fun interceptLiteModeLongClick(rootView: View): Boolean {
        return findLiteModeParent(rootView)?.let {
            it.performLongClick()
            true
        } ?: false
    }

    /**
     * Determines if the mapView needs to be wrapped in a traversal-blocking container.
     */
    private fun needsViewTraversalBlock(): Boolean {
        return callerPackageName in VIEW_TRAVERSAL_BLOCK_PACKAGES
    }

    /**
     * Finds the parent view that should receive redirected click events in liteMode.
     * Returns null if no redirection is needed for the current app.
     */
    private fun findLiteModeParent(rootView: View): ViewGroup? {
        val targetClass = LITE_MODE_CLICK_REDIRECT[callerPackageName] ?: return null
        val parentView = rootView.parent?.parent ?: return null
        if (parentView::class.qualifiedName == targetClass) {
            return parentView as? ViewGroup
        }
        return null
    }

    companion object {
        private const val TAG = "MapCompatAdapter"
        private val VIEW_TRAVERSAL_BLOCK_PACKAGES = setOf(
            "com.studioeleven.windfinder",
        )
        private val LITE_MODE_CLICK_REDIRECT = mapOf(
            "com.microsoft.teams" to "com.microsoft.teams.location.ui.map.MapViewLite",
        )
    }
}
